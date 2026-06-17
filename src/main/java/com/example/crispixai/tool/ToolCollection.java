package com.example.crispixai.tool;

import io.agentscope.core.tool.Toolkit;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Tool 汇总类
 * 统一创建 Toolkit 并注册所有自定义 Tool
 */
@Slf4j
@Component
@Getter
public class ToolCollection {

    @Resource
    private ImageGenerationTool imageGenerationTool;

    @Resource
    private PromptOptimizationTool promptOptimizationTool;

    private Toolkit toolkit;

    @PostConstruct
    public void init() {
        log.info("正在注册 Agent Tool...");

        toolkit = new Toolkit();

        // 图像生成
        toolkit.registerTool(imageGenerationTool);
        log.info("已注册 Tool: generate_image");

        // 提示词调优
        toolkit.registerTool(promptOptimizationTool);
        log.info("已注册 Tool: optimize_prompt");

        log.info("Agent Tool 注册完成");
    }


}