package com.example.crispixai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE 流式对话事件 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatEvent {

    /**
     * 事件类型: text_delta / tool_call / tool_result / thinking / done / error
     */
    private String type;

    /**
     * 事件内容
     */
    private String content;

    /**
     * 工具名称（仅 tool_call 类型时有值）
     */
    private String toolName;

    public ChatEvent(String type, String content) {
        this.type = type;
        this.content = content;
    }

    public static ChatEvent textDelta(String delta) {
        return new ChatEvent("text_delta", delta);
    }

    public static ChatEvent toolCall(String toolName) {
        ChatEvent event = new ChatEvent("tool_call", "");
        event.setToolName(toolName);
        return event;
    }

    public static ChatEvent done() {
        return new ChatEvent("done", "");
    }

    public static ChatEvent error(String message) {
        return new ChatEvent("error", message);
    }
}
