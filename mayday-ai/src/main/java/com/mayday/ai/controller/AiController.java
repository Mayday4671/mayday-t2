package com.mayday.ai.controller;

import com.mayday.ai.api.AiService;
import com.mayday.ai.api.StreamingAiService;
import com.mayday.ai.controller.req.CreateAiKeyReq;
import com.mayday.ai.model.vo.ChatReq;
import com.mayday.ai.model.vo.ChatRsp;
import com.mayday.ai.service.AiKeyService;
import com.mayday.common.web.AjaxResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 智能助手控制器
 * 支持同步和流式聊天
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiKeyService aiKeyService;
    private final AiService aiService;
    private final StreamingAiService streamingAiService;

    /** 流式任务线程池 */
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    /**
     * 同步聊天（原有接口）
     */
    @PostMapping("/chat")
    public AjaxResult chat(@RequestBody ChatReq req) {
        String result = aiService.chat(req.getSceneCode(), req.getTenantId(), req.getPrompt());
        return AjaxResult.success(new ChatRsp(result));
    }

    /**
     * 流式聊天（SSE）
     * 前端使用 EventSource 或 fetch 读取流式响应
     *
     * @param prompt    提示词
     * @param sceneCode 场景编码，默认 "chat"
     * @return SseEmitter 流式响应
     */
    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam String prompt,
            @RequestParam(defaultValue = "chat") String sceneCode) {
        
        // 3 分钟超时
        SseEmitter emitter = new SseEmitter(180_000L);
        
        streamExecutor.submit(() -> {
            try {
                streamingAiService.streamChat(sceneCode, "*", prompt,
                    // onToken: 每个 token 推送给前端
                    token -> {
                        try {
                            emitter.send(SseEmitter.event().data(token));
                        } catch (IOException e) {
                            log.warn("SSE 发送失败，客户端可能已断开: {}", e.getMessage());
                            emitter.completeWithError(e);
                        }
                    },
                    // onComplete: 完成时发送结束标记
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            log.warn("SSE 完成发送失败: {}", e.getMessage());
                        }
                    },
                    // onError: 错误处理
                    error -> {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            log.warn("SSE 错误发送失败: {}", e.getMessage());
                        }
                    }
                );
            } catch (Exception e) {
                log.error("流式聊天失败", e);
                emitter.completeWithError(e);
            }
        });
        
        // 超时回调
        emitter.onTimeout(() -> {
            log.warn("SSE 连接超时");
            emitter.complete();
        });
        
        // 错误回调
        emitter.onError(e -> {
            log.warn("SSE 连接错误: {}", e.getMessage());
        });
        
        return emitter;
    }

    /**
     * 测试插入 api-key
     */
    @PostMapping("/createKey")
    public AjaxResult createKey(@RequestBody CreateAiKeyReq req) {
        Long keyId = aiKeyService.createKey(
            req.getProvider(),
            req.getName(),
            req.getApiKey(),
            req.getRemark()
        );
        return AjaxResult.success(keyId);
    }
}

