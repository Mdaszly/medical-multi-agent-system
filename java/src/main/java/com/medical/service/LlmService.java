package com.medical.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

/**
 * LLM 调用的统一门面服务。
 * <p>
 * 封装了对 Spring AI ChatClient 和 DashScopeService 的双 Provider 调用逻辑，
 * 通过 {@code provider} 字段（配置项 {@code app.llm.provider}）在运行时决定路由目标——
 * 值为 {@code "dashscope"} 且 DashScope 已配置时走 DashScopeService，否则走 Spring AI ChatClient。
 * 该切换机制允许在不修改代码的情况下仅通过配置变更来切换底层 LLM 实现。
 */
@Slf4j
@Service
public class LlmService {

    /**
     * 默认 ChatClient，由 Spring AI 自动配置的 ChatModel 构建。
     */
    private final ChatClient chatClient;
    /**
     * 专用于问诊场景的 ChatClient，携带会话记忆 Advisor；
     * 若容器中未注入 {@code "consultChatClient"} 则降级回退为默认 {@code chatClient}。
     */
    private final ChatClient consultChatClient;
    private final DashScopeService dashScopeService;
    /**
     * 当前 LLM Provider 标识，经 {@code toLowerCase()} 处理以消除配置值大小写差异。
     */
    private final String provider;

    /**
     * 构造 LlmService。
     *
     * @param chatClientBuilder   Spring AI 自动注入的 ChatClient 构建器，用于创建默认 ChatClient
     * @param consultChatClient   可选注入的问诊专用 ChatClient（{@code @Autowired(required = false)}），
     *                            若容器中存在名为 {@code "consultChatClient"} 的 Bean 则使用之；
     *                            否则降级回退到默认 {@code chatClient}，确保问诊方法始终可用
     * @param dashScopeService    DashScope 原生 API 服务，用于直接调用 DashScope 接口
     * @param provider            LLM Provider 标识，默认值 {@code "dashscope"}，
     *                            经 {@code toLowerCase()} 处理后用于分支路由判断
     */
    public LlmService(
            ChatClient.Builder chatClientBuilder,
            @Autowired(required = false) @Qualifier("consultChatClient") ChatClient consultChatClient,
            DashScopeService dashScopeService,
            @Value("${app.llm.provider:dashscope}") String provider) {
        this.chatClient = chatClientBuilder.build();
        // consultChatClient 可选注入——若未配置则降级为默认 chatClient，保证问诊流程不因 Bean 缺失而中断
        this.consultChatClient = consultChatClient != null ? consultChatClient : this.chatClient;
        this.dashScopeService = dashScopeService;
        // 统一转小写，使配置值 "DashScope" / "dashscope" 等不同写法均能正确匹配分支条件
        this.provider = provider.toLowerCase();
    }

    /**
     * 同步生成 LLM 回复。
     * <p>
     * 分支路由逻辑：当 {@code provider} 为 {@code "dashscope"} 且 DashScope 已完成配置
     * （{@code dashScopeService.isConfigured()}）时，走 DashScope 原生 API 调用——
     * 避免经过 Spring AI 适配层以获得更精确的参数控制与更低延迟；
     * 否则走 Spring AI ChatClient，由其内部适配的 ChatModel 执行调用。
     *
     * @param systemPrompt 系统提示词，设定 LLM 行为角色与约束
     * @param userPrompt   用户提示词，包含实际请求内容
     * @return LLM 生成的文本内容
     * @throws Exception 调用 LLM 过程中的异常
     */
    public String generate(String systemPrompt, String userPrompt) throws Exception {
        log.info("Using LLM provider: {}", provider);
        // provider=dashscope 且 DashScope 已配置时优先使用原生接口，以绕过 Spring AI 适配层获得更细粒度控制
        if ("dashscope".equals(provider) && dashScopeService.isConfigured()) {
            return dashScopeService.generate(systemPrompt, userPrompt);
        }
        return chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
    }

    /**
     * 流式生成 LLM 回复，逐 chunk 回调消费。
     * <p>
     * 分支路由逻辑与 {@link #generate} 一致：DashScope 可用时走其原生流式接口，
     * 否则走 Spring AI Flux 流式分支。
     * Spring AI 分支通过 {@code flux.doOnNext(onChunk).blockLast()} 将异步 Flux 流
     * 转换为同步逐块回调——{@code doOnNext} 确保每个 chunk 到达时立即通知消费者，
     * {@code blockLast()} 阻塞至流结束以维持方法同步语义。
     *
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param onChunk      chunk 回调，每收到一段文本即触发消费
     * @throws Exception 调用 LLM 过程中的异常
     */
    public void generateStream(String systemPrompt, String userPrompt, Consumer<String> onChunk) throws Exception {
        // 同 generate：DashScope 可用时走原生流式接口，参数传递更直接
        if ("dashscope".equals(provider) && dashScopeService.isConfigured()) {
            dashScopeService.generateStream(systemPrompt, userPrompt, onChunk);
            return;
        }
        Flux<String> flux = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
        // doOnNext 逐 chunk 即时回调消费者；blockLast 阻塞至流结束，保持方法同步语义
        flux.doOnNext(onChunk).blockLast();
    }

    /**
     * 带会话记忆的流式生成方法。
     * <p>
     * 使用 {@code consultChatClient}（而非普通 {@code chatClient}），因为问诊专用 ChatClient
     * 在构建时已挂载 {@code MessageChatMemoryAdvisor} 及对应的 {@code ChatMemory} Bean，
     * 使得多轮对话历史能自动注入 Prompt 上下文——普通 ChatClient 无此 Advisor，无法维持记忆。
     * <p>
     * 通过 {@code advisors(spec -> spec.param(CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))} 将会话 ID
     * 传入 Advisor，Advisor据此从 ChatMemory 中检索该会话的历史消息并拼接到 Prompt 中，
     * 实现「同一会话内多轮上下文连续」的效果。
     *
     * @param sessionId    会话标识，用于 ChatMemory 按会话隔离检索历史消息
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户提示词
     * @param onChunk      chunk 回调
     */
    public void generateStreamWithMemory(String sessionId, String systemPrompt, String userPrompt,
                                         Consumer<String> onChunk) {
        Flux<String> flux = consultChatClient.prompt()
                // 将 sessionId 注入 Advisor 参数，使其按会话 ID 从 ChatMemory 检索历史消息拼入 Prompt——这是多轮记忆生效的关键
                .advisors(spec -> spec.param(MessageChatMemoryAdvisor.CHAT_MEMORY_CONVERSATION_ID_KEY, sessionId))
                .system(systemPrompt)
                .user(userPrompt)
                .stream()
                .content();
        // 同 generateStream：doOnNext 即时回调 + blockLast 阻塞至流结束，保持同步语义
        flux.doOnNext(onChunk).blockLast();
    }

    /**
     * 返回当前生效的 LLM Provider 标识。
     *
     * @return provider 名称，如 {@code "dashscope"} 或 {@code "spring-ai"}
     */
    public String getProvider() {
        return provider;
    }
}
