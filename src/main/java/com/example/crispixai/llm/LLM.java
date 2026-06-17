package com.example.crispixai.llm;

import com.example.crispixai.config.CrispixConfig;

import com.example.crispixai.tool.ToolCollection;
import io.agentscope.core.studio.StudioManager;
import io.agentscope.core.studio.StudioMessageHook;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.core.model.ModelRegistry;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.formatter.openai.OpenAIChatFormatter;
import io.agentscope.core.formatter.anthropic.AnthropicChatFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.nio.file.Paths;

/**
 * LLM 业务配置类
 * 注册模型提供商并创建 HarnessAgent Bean
 */
@Slf4j
@Configuration
public class LLM {

    private final CrispixConfig config;
    private final ToolCollection toolCollection;



    public LLM(CrispixConfig config, ToolCollection toolCollection) {
        this.config = config;
        this.toolCollection = toolCollection;
    }

    /**
     * 应用启动时注册模型提供商到 ModelRegistry
     */
    @PostConstruct
    public void registerModels() {
        log.info("正在注册 AgentScope 模型提供商...");

        // 注册 OpenAI 及兼容接口（DeepSeek、Kimi、vLLM 等）
        registerOpenAI();

        // 注册 Anthropic
        registerAnthropic();

        log.info("AgentScope 模型注册完成");
    }

    /**
     * 应用启动时初始化 Studio 连接（只执行一次）
     */
    @PostConstruct
    public void initStudio() {
        if (config.isStudioEnabled()) {
            log.info("正在初始化 Studio 连接: {}", config.getStudioUrl());
            StudioManager.init()
                .studioUrl(config.getStudioUrl())
                .project(config.getStudioProject())
                .runName(config.getStudioRunName())
                .initialize()
                .block();
            log.info("Studio 连接初始化完成");
        } else {
            log.info("Studio 未启用，跳过初始化");
        }
    }

    /**
     * 注册 OpenAI 及兼容接口
     */
    private void registerOpenAI() {
        String apiKey = config.getOpenaiApiKey();
        String baseUrl = config.getOpenaiBaseUrl();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("OpenAI API Key 未配置，跳过注册");
            return;
        }

        ModelRegistry.registerFactory(
            "openai:.*",
            modelId -> {
                String modelName = modelId.substring("openai:".length());
                log.info("创建 OpenAI 模型: {}", modelName);
                return OpenAIChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .baseUrl(baseUrl)
                    .stream(true)
                    .formatter(new OpenAIChatFormatter())
                    .build();
            }
        );

        log.info("OpenAI 模型提供商已注册，Base URL: {}", baseUrl);
    }

    /**
     * 注册 Anthropic
     */
    private void registerAnthropic() {
        String apiKey = config.getAnthropicApiKey();

        if (apiKey == null || apiKey.isEmpty()) {
            log.warn("Anthropic API Key 未配置，跳过注册");
            return;
        }

        ModelRegistry.registerFactory(
            "anthropic:.*",
            modelId -> {
                String modelName = modelId.substring("anthropic:".length());
                log.info("创建 Anthropic 模型: {}", modelName);
                return AnthropicChatModel.builder()
                    .apiKey(apiKey)
                    .modelName(modelName)
                    .stream(true)
                    .formatter(new AnthropicChatFormatter())
                    .build();
            }
        );

        log.info("Anthropic 模型提供商已注册");
    }

    /**
     * 创建 HarnessAgent 单例 Bean
     */
    @Bean
    public HarnessAgent harnessAgent() {
        log.info("正在创建 HarnessAgent Bean...");

        // 构建harness（React模式智能体）
        HarnessAgent agent = HarnessAgent.builder()
            .name(config.getAgentName())
            .model(config.getModel())
            .workspace(Paths.get(config.getWorkspacePath()))
            .toolkit(toolCollection.getToolkit())
            .hook(config.isStudioEnabled()
                ? new StudioMessageHook(StudioManager.getClient())
                : null)
            .compaction(CompactionConfig.builder()
                .triggerMessages(30)
                .keepMessages(10)
                .build())
            .build();

        log.info("HarnessAgent 创建完成 - 名称: {}, 模型: {}, 工作区: {}",
            config.getAgentName(),
            config.getModel(),
            config.getWorkspacePath());

        return agent;
    }
}