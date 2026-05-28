package com.medical.memory;

import com.medical.constant.ConsultConstant;
import com.medical.model.vo.ChatMessageVO;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 将数据库中的会话消息格式化为可拼进 LLM Prompt 的纯文本块。
 * <p>供 {@link com.medical.service.ConsultMemoryService#loadHistoryPrompt} 与 Pipeline 使用。</p>
 */
public final class ConsultHistoryFormatter {

    private ConsultHistoryFormatter() {
    }

    /**
     * String format(List&lt;ChatMessageVO&gt; messages)
     * <p>输出形如「历史对话：\n用户：…\n助手：…」的字符串；无消息时返回空串。</p>
     */
    public static String format(List<ChatMessageVO> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("历史对话：\n");
        for (ChatMessageVO message : messages) {
            if (!StringUtils.hasText(message.getContent())) {
                continue;
            }
            String roleLabel = ConsultConstant.ROLE_ASSISTANT.equals(message.getRole()) ? "助手" : "用户";
            sb.append(roleLabel).append("：").append(message.getContent()).append("\n");
        }
        return sb.toString();
    }
}
