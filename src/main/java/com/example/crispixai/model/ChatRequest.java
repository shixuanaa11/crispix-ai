package com.example.crispixai.model;

import lombok.Data;

/**
 * 对话请求 DTO
 */
@Data
public class ChatRequest {

    /**
     * 会话 ID，用于维持对话上下文
     */
    private String sessionId;

    /**
     * 用户输入的消息
     */
    private String message;

    /**
     * 临时切换模型（可选），格式如 openai:gpt-4o
     */
    private String model;
}
