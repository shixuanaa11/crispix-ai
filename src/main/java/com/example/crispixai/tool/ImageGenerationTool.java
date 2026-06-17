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
 * 图像生成 Tool
 * 调用 DashScope 文生图接口，根据提示词生成图像
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ImageGenerationTool {

    private static final MediaType JSON_MEDIA_TYPE = MediaType.get("application/json; charset=utf-8");
    private static final String DEFAULT_SIZE = "1024*1024";
    private static final String DEFAULT_MODEL = "wanx2.1-t2i-turbo";
    private static final String IMAGE_API_URL =
            "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";

    private final CrispixConfig crispixConfig;
    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build();

    @Tool(
            name = "generate_image",
            description = "根据文本提示词生成图像，适用于插画、海报、产品图、概念设计等场景。",
            readOnly = false,
            concurrencySafe = false)
    public String generateImage(
            @ToolParam(name = "prompt", description = "图像生成提示词，需描述主体、场景、风格、光线和构图")
            String prompt,
            @ToolParam(name = "size", description = "图像尺寸，如 1024*1024、720*1280，默认 1024*1024")
            String size,
            @ToolParam(name = "style", description = "图像风格补充，如 写实、动漫、油画、极简，可选")
            String style) {

        if (!StringUtils.hasText(prompt)) {
            return "错误：prompt 不能为空";
        }
        if (!StringUtils.hasText(crispixConfig.getOpenaiApiKey())) {
            return "错误：未配置 agentscope.openai.api-key，无法调用图像生成接口";
        }

        String finalPrompt = StringUtils.hasText(style) ? prompt + "，风格：" + style : prompt;
        String imageSize = StringUtils.hasText(size) ? size : DEFAULT_SIZE;

        JSONObject body = new JSONObject();
        body.put("model", DEFAULT_MODEL);
        body.put("input", new JSONObject().fluentPut("prompt", finalPrompt));
        body.put("parameters", new JSONObject()
                .fluentPut("size", imageSize)
                .fluentPut("n", 1));

        Request request = new Request.Builder()
                .url(IMAGE_API_URL)
                .addHeader("Authorization", "Bearer " + crispixConfig.getOpenaiApiKey())
                .addHeader("Content-Type", "application/json")
                .addHeader("X-DashScope-Async", "enable")
                .post(RequestBody.create(body.toJSONString(), JSON_MEDIA_TYPE))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String responseBody = response.body() != null ? response.body().string() : "";
            if (!response.isSuccessful()) {
                log.error("图像生成失败，status={}, body={}", response.code(), responseBody);
                return "图像生成失败：" + response.code() + " " + responseBody;
            }

            JSONObject json = JSON.parseObject(responseBody);
            JSONObject output = json.getJSONObject("output");
            if (output == null) {
                return "图像生成失败：响应缺少 output 字段，原始响应=" + responseBody;
            }

            String taskId = output.getString("task_id");
            if (StringUtils.hasText(taskId)) {
                return "图像生成任务已提交，task_id=" + taskId + "。请稍后查询任务结果。";
            }

            JSONArray results = output.getJSONArray("results");
            if (results != null && !results.isEmpty()) {
                JSONObject first = results.getJSONObject(0);
                String url = first.getString("url");
                if (StringUtils.hasText(url)) {
                    return "图像生成成功，图片地址：" + url;
                }
            }

            return "图像生成完成，但未返回可用图片地址，原始响应=" + responseBody;
        } catch (Exception e) {
            log.error("图像生成异常", e);
            return "图像生成异常：" + e.getMessage();
        }
    }
}
