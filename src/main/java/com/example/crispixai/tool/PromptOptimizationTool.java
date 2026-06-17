package com.example.crispixai.tool;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.crispixai.config.CrispixConfig;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;

/**
 * 提示词调优 Tool
 * 调用大模型对原始提示词进行结构化优化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptOptimizationTool {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");

    private final CrispixConfig crispixConfig;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    @Tool(
            name = "optimize_prompt",
            description = "优化用户提供的提示词，使其更适合图像生成或大模型对话。",
            readOnly = true,
            concurrencySafe = true)
    public String optimizePrompt(
            @ToolParam(name = "original_prompt", description = "需要优化的原始提示词")
            String originalPrompt,
            @ToolParam(name = "scenario", description = "使用场景：image（图像生成）或 chat（对话），默认 image")
            String scenario,
            @ToolParam(name = "language", description = "输出语言，如 zh 或 en，默认 zh")
            String language) {

        if (!StringUtils.hasText(originalPrompt)) {
            return "错误：original_prompt 不能为空";
        }
        if (!StringUtils.hasText(crispixConfig.getOpenaiApiKey())) {
            return "错误：未配置 agentscope.openai.api-key，无法调用提示词优化接口";
        }

        String useScenario = StringUtils.hasText(scenario) ? scenario : "image";
        String outputLanguage = StringUtils.hasText(language) ? language : "zh";
        String systemPrompt = buildSystemPrompt(useScenario, outputLanguage);
        String chatUrl = normalizeChatCompletionsUrl(crispixConfig.getOpenaiBaseUrl());

        JSONArray messages = new JSONArray();
        messages.add(new JSONObject().fluentPut("role", "system").fluentPut("content", systemPrompt));
        messages.add(new JSONObject().fluentPut("role", "user").fluentPut("content", originalPrompt));

        JSONObject body = new JSONObject();
        body.put("model", extractModelName(crispixConfig.getModel()));
        body.put("temperature", 0.7);
        body.put("messages", messages);

        Request request = new Request.Builder()
                .url(chatUrl)
                .addHeader("Authorization", "Bearer " + crispixConfig.getOpenaiApiKey())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("提示词优化失败，status={}, body={}", response.code(), responseBody);
                return "提示词优化失败：" + response.code() + " " + responseBody;
            }

            JSONObject json = JSON.parseObject(responseBody);
            JSONArray choices = json.getJSONArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "提示词优化失败：响应缺少 choices 字段，原始响应=" + responseBody;
            }

            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            String content = message != null ? message.getString("content") : null;
            if (!StringUtils.hasText(content)) {
                return "提示词优化失败：模型未返回有效内容，原始响应=" + responseBody;
            }
            return content.trim();
        } catch (Exception e) {
            log.error("提示词优化异常", e);
            return "提示词优化异常：" + e.getMessage();
        }
    }

    private String buildSystemPrompt(String scenario, String language) {
        if ("chat".equalsIgnoreCase(scenario)) {
            return """
                    你是提示词工程专家。请优化用户提供的对话类提示词。
                    要求：
                    1. 保留用户原始意图
                    2. 明确角色、目标、约束和输出格式
                    3. 删除歧义表达，补充必要上下文
                    4. 输出语言使用 %s
                    5. 只返回优化后的提示词，不要解释过程
                    """.formatted(language);
        }

        return """
                你是图像提示词优化专家。请优化用户提供的文生图提示词。
                要求：
                1. 保留用户原始意图
                2. 补充主体、场景、构图、镜头、光线、材质、风格和质量词
                3. 避免冲突描述，控制长度在 120 字以内
                4. 输出语言使用 %s
                5. 只返回优化后的提示词，不要解释过程
                """.formatted(language);
    }

    private String normalizeChatCompletionsUrl(String baseUrl) {
        String normalized = StringUtils.hasText(baseUrl) ? baseUrl.trim() : "https://api.openai.com/v1";
        if (normalized.endsWith("/chat/completions")) {
            return normalized;
        }
        if (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized + "/chat/completions";
    }

    private String extractModelName(String modelId) {
        if (!StringUtils.hasText(modelId)) {
            return "gpt-4o";
        }
        int index = modelId.indexOf(':');
        return index >= 0 ? modelId.substring(index + 1) : modelId;
    }
}
