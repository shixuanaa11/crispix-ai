package com.example.crispixai.service;

import com.example.crispixai.model.ChatEvent;
import io.agentscope.core.message.Msg;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Agent 对话服务接口
 */
public interface AgentService {

    /**
     * 同步对话
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户输入
     * @return 完整的回复消息
     */
    Mono<Msg> chat(String userId, String sessionId, String message);

    /**
     * 流式对话
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @param message   用户输入
     * @return SSE 事件流
     */
    Flux<ChatEvent> chatStream(String userId, String sessionId, String message);

    /**
     * 获取对话历史记录
     *
     * @param userId    用户 ID
     * @param sessionId 会话 ID
     * @return 对话历史
     */
    String getConversationHistory(String userId, String sessionId);
}