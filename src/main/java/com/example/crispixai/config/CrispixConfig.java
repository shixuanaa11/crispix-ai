package com.example.crispixai.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Crispix 配置类
 * 读取 application.yml 中 agentscope 相关配置
 */
@Getter
@Configuration
public class CrispixConfig {

    @Value("${agentscope.agent-name:crispix-assistant}")
    private String agentName;

    @Value("${agentscope.model:openai:gpt-4o}")
    private String model;

    @Value("${agentscope.workspace-path:./.agentscope/workspace}")
    private String workspacePath;

    @Value("${agentscope.openai.api-key:}")
    private String openaiApiKey;

    @Value("${agentscope.openai.base-url:https://api.openai.com/v1}")
    private String openaiBaseUrl;

    @Value("${agentscope.anthropic.api-key:}")
    private String anthropicApiKey;

    // ========== AgentScope Studio 配置 ==========

    @Value("${agentscope.studio.enabled:false}")
    private boolean studioEnabled;

    @Value("${agentscope.studio.url:http://localhost:3000}")
    private String studioUrl;

    @Value("${agentscope.studio.project:crispix-ai}")
    private String studioProject;

    @Value("${agentscope.studio.run-name:crispix-run}")
    private String studioRunName;
}