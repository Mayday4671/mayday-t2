package com.mayday.ai.api.impl;

import com.mayday.ai.api.StreamingAiService;
import com.mayday.ai.factory.impl.AiModelFactoryImpl;
import com.mayday.ai.model.entity.AiConfigEntity;
import com.mayday.ai.router.impl.AiRouterBf;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

/**
 * 流式 AI 服务实现
 * 使用 LangChain4j StreamingChatLanguageModel 实现边生成边推送
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingAiServiceImpl implements StreamingAiService {

    private final AiRouterBf aiRouterBf;
    private final AiModelFactoryImpl aiModelFactory;

    @Override
    public void streamChat(String sceneCode, String tenantId, String prompt,
                           Consumer<String> onToken, Runnable onComplete, Consumer<Throwable> onError) {
        if (prompt == null || prompt.isBlank()) {
            onError.accept(new IllegalArgumentException("prompt is blank"));
            return;
        }

        List<AiConfigEntity> list = aiRouterBf.route(tenantId, sceneCode);
        if (list == null || list.isEmpty()) {
            onError.accept(new IllegalStateException("No ai_config found for tenant=" + tenantId + ", scene=" + sceneCode));
            return;
        }

        AiConfigEntity cfg = list.getFirst();
        log.debug("流式调用 AI 模型 [provider={}, model={}]", cfg.getProvider(), cfg.getModelName());

        try {
            StreamingChatLanguageModel model = aiModelFactory.getOrCreateStreaming(cfg);
            
            model.generate(prompt, new StreamingResponseHandler<AiMessage>() {
                @Override
                public void onNext(String token) {
                    // 每个 token 推送给前端
                    onToken.accept(token);
                }

                @Override
                public void onComplete(Response<AiMessage> response) {
                    log.debug("流式生成完成，总 tokens: {}", 
                        response.tokenUsage() != null ? response.tokenUsage().totalTokenCount() : "unknown");
                    onComplete.run();
                }

                @Override
                public void onError(Throwable error) {
                    log.error("流式生成失败", error);
                    onError.accept(error);
                }
            });
        } catch (Exception e) {
            log.error("创建流式模型失败", e);
            onError.accept(e);
        }
    }
}
