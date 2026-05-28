package com.medical.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.medical.common.ErrorCode;
import com.medical.constant.ConsultConstant;
import com.medical.exception.ThrowUtils;
import com.medical.mapper.ChatMessageMapper;
import com.medical.mapper.ChatSessionMapper;
import com.medical.model.entity.ChatMessage;
import com.medical.model.entity.ChatSession;
import com.medical.model.vo.ChatMessageVO;
import com.medical.model.vo.ChatSessionHistoryVO;
import com.medical.model.vo.ChatSessionVO;
import com.medical.service.ChatSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 问诊会话与消息管理服务实现。
 * <p>
 * 基于 MyBatis-Plus {@link ServiceImpl} 提供 ChatSession 的 CRUD 操作，
 * 并通过 {@link ChatMessageMapper} 管理会话内的消息记录。
 * 所有涉及会话读/写/删除的公开方法均内置「归属校验」逻辑（{@link #getOwnedSession}），
 * 确保用户只能操作自己创建的会话，防止越权访问。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl extends ServiceImpl<ChatSessionMapper, ChatSession>
        implements ChatSessionService {

    private final ChatMessageMapper chatMessageMapper;

    /**
     * 创建新会话，返回含 UUID 的会话视图。
     * <p>
     * scene 和 title 若未传入则使用默认值（{@link ConsultConstant#SCENE_CONSULTATION}、
     * {@link ConsultConstant#DEFAULT_SESSION_TITLE}），避免前端遗漏必填项导致创建失败。
     *
     * @param userId  会话所属用户 ID
     * @param scene   问诊场景标识，为空时默认为咨询场景
     * @param title   会话标题，为空时使用默认标题
     * @return {@link ChatSessionVO} 包含完整会话信息的视图对象
     */
    @Override
    public ChatSessionVO createSession(Long userId, String scene, String title) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        // scene/title 为空时回退到默认值，降低前端传参约束
        String resolvedScene = StringUtils.hasText(scene) ? scene : ConsultConstant.SCENE_CONSULTATION;
        String resolvedTitle = StringUtils.hasText(title) ? title : ConsultConstant.DEFAULT_SESSION_TITLE;

        ChatSession session = new ChatSession();
        session.setSessionId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setScene(resolvedScene);
        session.setTitle(resolvedTitle);
        save(session);
        return ChatSessionVO.fromEntity(session);
    }

    /**
     * 获取指定会话并校验归属权。
     * <p>
     * 通过 {@link #getOwnedSession} 查询并隐式验证 sessionId + userId 的联合匹配，
     * 若不存在或不属于当前用户则抛出参数异常。
     *
     * @param sessionId 会话 UUID
     * @param userId    当前用户 ID
     * @return 属于该用户的会话实体
     */
    @Override
    public ChatSession getSessionForUser(String sessionId, Long userId) {
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        return session;
    }

    /**
     * 异步更新会话标题（仅当原标题为空时才写入）。
     * <p>
     * 使用 {@code @Async} 在独立线程池执行，避免阻塞主流程（如问诊 Pipeline）。
     * 仅在会话尚未设置标题时才用首条用户消息内容填充——
     * 此策略确保用户后续手动修改的标题不会被自动生成的标题覆盖。
     * 参数不合法或会话不存在时静默返回，不抛异常，保证异步任务的容错性。
     *
     * @param sessionId 会话 UUID
     * @param title     期望设置的标题（通常为首条用户消息摘要）
     * @param userId    用户 ID
     */
    @Async
    @Override
    public void updateAsync(String sessionId, String title, Long userId) {
        // 参数不合法时静默跳过——异步方法不宜抛异常影响主流程
        if (!StringUtils.hasText(sessionId) || userId == null) {
            return;
        }
        ChatSession session = lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .one();
        // 会话不存在也静默跳过，异步场景下无需中断
        if (session == null) {
            return;
        }
        // 仅当原标题为空且有新标题时才写入——防止自动标题覆盖用户手动修改的标题
        if (!StringUtils.hasText(session.getTitle()) && StringUtils.hasText(title)) {
            String trimmed = title.length() > 100 ? title.substring(0, 100) : title;
            session.setTitle(trimmed);
        }
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
        log.debug("Session title updated async: {}", sessionId);
    }

    /**
     * 查询用户的会话历史，按时间区间分组返回。
     * <p>
     * 从数据库按 {@code updateTime} 降序拉取最多 {@link ConsultConstant#HISTORY_MAX_SIZE} 条会话，
     * 然后在内存中按「今天 / 最近30天 / 最近一年 / 更早」四档分组——
     * 此分组策略与前端侧边栏展示结构对齐，避免前端二次排序。
     * updateTime 为空时回退到 createTime，两者均为空则归入「更早」档。
     *
     * @param userId 用户 ID
     * @return {@link ChatSessionHistoryVO} 按时间档位分组的会话列表
     */
    @Override
    public ChatSessionHistoryVO queryHistory(Long userId) {
        ThrowUtils.throwIf(userId == null || userId <= 0, ErrorCode.PARAM_ERROR, "用户ID无效");

        // 按更新时间降序取有限条数，避免历史过多时全量加载
        List<ChatSession> sessions = lambdaQuery()
                .eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime)
                .last("LIMIT " + ConsultConstant.HISTORY_MAX_SIZE)
                .list();

        // 以今天零点为基准计算三档时间边界，与前端侧边栏分组结构对齐
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime thirtyDaysAgo = startOfToday.minusDays(30);
        LocalDateTime oneYearAgo = startOfToday.minusYears(1);

        ChatSessionHistoryVO history = new ChatSessionHistoryVO();
        for (ChatSession session : sessions) {
            ChatSessionVO vo = ChatSessionVO.fromEntity(session);
            // updateTime 为空时回退到 createTime，保证无更新记录的旧会话仍可归类
            LocalDateTime updateTime = session.getUpdateTime() != null
                    ? session.getUpdateTime() : session.getCreateTime();
            // 两者均为空则无法归类，直接归入最远档
            if (updateTime == null) {
                history.getOlderThanYear().add(vo);
                continue;
            }
            if (!updateTime.isBefore(startOfToday)) {
                history.getToday().add(vo);
            } else if (!updateTime.isBefore(thirtyDaysAgo)) {
                history.getLast30Days().add(vo);
            } else if (!updateTime.isBefore(oneYearAgo)) {
                history.getLastYear().add(vo);
            } else {
                history.getOlderThanYear().add(vo);
            }
        }
        return history;
    }

    /**
     * 删除会话及其全部消息。
     * <p>
     * 使用事务保证「先删消息、再删会话」的一致性——若消息删除失败则会话也不被删除，
     * 防止出现无主消息的孤儿数据。
     *
     * @param sessionId 会话 UUID
     * @param userId    当前用户 ID，用于归属校验
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Long userId) {
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        // 先删消息再删会话——事务内顺序保证不会遗留孤儿消息
        chatMessageMapper.deleteBySessionId(sessionId);
        removeById(session.getId());
    }

    /**
     * 手动更新会话标题。
     * <p>
     * 与 {@link #updateAsync} 的「仅空标题时写入」策略不同，本方法允许强制覆盖标题，
     * 供用户主动修改会话名称的场景使用。标题长度超过 100 字符时截断，防止数据库字段溢出。
     *
     * @param sessionId 会话 UUID
     * @param title     新标题，不允许为空
     * @param userId    当前用户 ID
     */
    @Override
    public void updateTitle(String sessionId, String title, Long userId) {
        ThrowUtils.throwIf(!StringUtils.hasText(title), ErrorCode.PARAM_ERROR, "标题不能为空");
        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");
        // 截断超长标题，防止数据库字段溢出
        session.setTitle(title.length() > 100 ? title.substring(0, 100) : title);
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);
    }

    /**
     * 保存一条聊天消息并同步更新会话的 updateTime。
     * <p>
     * 使用事务保证「消息入库 + 会话时间戳刷新」的一致性——
     * 消息保存成功但会话时间戳未更新时，前端历史列表排序会与实际消息时间不一致。
     * agentType、riskLevel、metadataJson 为可选字段，允许为 null（如用户消息不含这些字段）。
     *
     * @param sessionId    会话 UUID
     * @param userId       当前用户 ID，用于归属校验
     * @param role         消息角色（user / assistant / system 等）
     * @param content      消息正文
     * @param agentType    产生该消息的 Agent 类型标识，可为 null
     * @param riskLevel    风险等级标识，可为 null
     * @param metadataJson 附加元数据 JSON，可为 null
     * @return {@link ChatMessageVO} 含完整消息信息的视图对象
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessageVO saveMessage(String sessionId, Long userId, String role, String content,
                                     String agentType, String riskLevel, String metadataJson) {
        ThrowUtils.throwIf(!StringUtils.hasText(sessionId), ErrorCode.PARAM_ERROR, "会话ID不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(role), ErrorCode.PARAM_ERROR, "消息角色不能为空");
        ThrowUtils.throwIf(!StringUtils.hasText(content), ErrorCode.PARAM_ERROR, "消息内容不能为空");

        ChatSession session = getOwnedSession(sessionId, userId);
        ThrowUtils.throwIf(session == null, ErrorCode.PARAM_ERROR, "会话不存在或无权访问");

        ChatMessage message = new ChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setAgentType(agentType);
        message.setRiskLevel(riskLevel);
        message.setMetadataJson(metadataJson);
        chatMessageMapper.insert(message);

        // 刷新会话 updateTime，使前端历史列表按最新消息时间排序
        session.setUpdateTime(LocalDateTime.now());
        updateById(session);

        return ChatMessageVO.fromEntity(message);
    }

    /**
     * 查询指定会话的消息列表（按发送时间升序）。
     * <p>
     * 先通过 {@link #getSessionForUser} 校验归属权，再按时间升序拉取消息。
     * limit 参数经安全处理：非法值（≤0）默认取 100，超出上限截断至 200，
     * 防止前端传入超大 limit 导致全表扫描。
     *
     * @param sessionId 会话 UUID
     * @param userId    当前用户 ID
     * @param limit     期望拉取条数，≤0 时默认 100，上限 200
     * @return 按时间升序排列的消息视图列表
     */
    @Override
    public List<ChatMessageVO> listMessages(String sessionId, Long userId, int limit) {
        getSessionForUser(sessionId, userId);
        // limit 安全处理：非法值默认 100，上限截断至 200，防止前端传入超大值导致性能问题
        int resolvedLimit = limit <= 0 ? 100 : Math.min(limit, 200);

        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
                        .last("LIMIT " + resolvedLimit)
        );
        return messages.stream().map(ChatMessageVO::fromEntity).collect(Collectors.toList());
    }

    /**
     * 按 sessionId + userId 联合查询会话，隐式完成归属校验。
     * <p>
     * 同时匹配 sessionId 与 userId 而非仅匹配 sessionId——
     * 即使 sessionId 泄露，其他用户也无法通过它获取不属于自己的会话。
     * 参数不合法时返回 null，由调用方决定是抛异常还是静默处理。
     *
     * @param sessionId 会话 UUID
     * @param userId    用户 ID
     * @return 属于该用户的会话实体，不存在或不属于该用户时返回 null
     */
    private ChatSession getOwnedSession(String sessionId, Long userId) {
        if (!StringUtils.hasText(sessionId) || userId == null) {
            return null;
        }
        // sessionId + userId 联合查询而非仅 sessionId——防止 sessionId 泄露后的越权访问
        return lambdaQuery()
                .eq(ChatSession::getSessionId, sessionId)
                .eq(ChatSession::getUserId, userId)
                .one();
    }
}
