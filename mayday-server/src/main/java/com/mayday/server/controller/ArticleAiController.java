package com.mayday.server.controller;

import com.mayday.ai.api.AiService;
import com.mayday.ai.api.StreamingAiService;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 文章生成控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/article/ai")
@RequiredArgsConstructor
public class ArticleAiController {

    private final AiService aiService;
    private final StreamingAiService streamingAiService;
    private final ICrawlerArticleService articleService;

    private final ExecutorService streamExecutor = Executors.newCachedThreadPool();

    /**
     * 生成文章（同步）
     */
    @PostMapping("/generate")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult generate(@RequestBody GenerateReq req) {
        String prompt = buildGeneratePrompt(req.getTopic(), req.getKeywords(), req.getStyle());
        try {
            String result = aiService.chat("article_generation", "*", prompt);
            Map<String, String> parsed = parseGeneratedContent(result);
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    /**
     * 生成文章（流式 SSE）
     */
    @GetMapping(value = "/generate/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter generateStream(@RequestParam("topic") String topic, 
                                     @RequestParam(value = "keywords", required = false) String keywords, 
                                     @RequestParam(value = "style", required = false) String style) {
        String prompt = buildGeneratePrompt(topic, keywords, style);
        return handleStreamRequest(prompt);
    }

    /**
     * 优化文章（同步）
     */
    @PostMapping("/optimize")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult optimize(@RequestBody OptimizeReq req) {
        String prompt = buildOptimizePrompt(req.getTitle(), req.getContent());
        try {
            String result = aiService.chat("article_generation", "*", prompt);
            Map<String, String> parsed = parseGeneratedContent(result);
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    /**
     * 优化文章（流式 SSE）
     */
    @PostMapping(value = "/optimize/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter optimizeStream(@RequestBody OptimizeReq req) {
        String prompt = buildOptimizePrompt(req.getTitle(), req.getContent());
        return handleStreamRequest(prompt);
    }

    /**
     * 修正文章（同步）
     */
    @PostMapping("/correct")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult correct(@RequestBody CorrectReq req) {
        String prompt = buildCorrectPrompt(req.getTitle(), req.getContent(), req.getCorrection());
        try {
            String result = aiService.chat("article_generation", "*", prompt);
            Map<String, String> parsed = parseGeneratedContent(result);
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    /**
     * 修正文章（流式 SSE）
     */
    @PostMapping(value = "/correct/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    public SseEmitter correctStream(@RequestBody CorrectReq req) {
        String prompt = buildCorrectPrompt(req.getTitle(), req.getContent(), req.getCorrection());
        return handleStreamRequest(prompt);
    }
    
    // ========== Helper Methods for Stream ==========

    private SseEmitter handleStreamRequest(String prompt) {
        // 5分钟超时（文章生成比较慢）
        SseEmitter emitter = new SseEmitter(300_000L);
        
        streamExecutor.submit(() -> {
            try {
                streamingAiService.streamChat("article_generation", "*", prompt,
                    token -> {
                        try {
                            // 包装为 JSON 以保留换行符等特殊字符
                            Map<String, String> data = new HashMap<>();
                            data.put("content", token);
                            emitter.send(SseEmitter.event().data(data));
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    () -> {
                        try {
                            emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                            emitter.complete();
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    },
                    error -> {
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                            emitter.completeWithError(error);
                        } catch (IOException e) {
                            emitter.completeWithError(e);
                        }
                    }
                );
            } catch (Exception e) {
                log.error("流式生成失败", e);
                emitter.completeWithError(e);
            }
        });
        
        return emitter;
    }

    private String buildOptimizePrompt(String title, String content) {
        return "请优化以下文章内容，使其更加专业、流畅、有吸引力：\n\n" +
                "标题：" + title + "\n\n" +
                "正文：" + content + "\n\n" +
                "请保持技术教程风格，输出格式：\n【标题】xxx\n【摘要】xxx\n【正文】xxx";
    }

    private String buildCorrectPrompt(String title, String content, String correction) {
        return "请根据以下修改意见修正文章：\n\n" +
                "原标题：" + title + "\n" +
                "原正文：" + content + "\n\n" +
                "修改意见：" + correction + "\n\n" +
                "请输出修正后的内容，格式：\n【标题】xxx\n【摘要】xxx\n【正文】xxx";
    }

    private AjaxResult handleAiException(Exception e) {
        String msg = e.getMessage();
        if (msg != null && (msg.contains("quota") || msg.contains("429") || msg.contains("RESOURCE_EXHAUSTED"))) {
            return AjaxResult.error("AI 服务调用受限：当前 API Key 配额已耗尽，请稍后再试或更换 Key。");
        }
        if (msg != null && (msg.contains("timeout") || msg.contains("aborted"))) {
            return AjaxResult.error("AI 服务响应超时，请重试。");
        }
        e.printStackTrace();
        return AjaxResult.error("AI 服务调用失败: " + msg);
    }

    /**
     * 保存 AI 生成的文章
     */
    @PostMapping("/save")
    @PreAuthorize("isAuthenticated()")
    public AjaxResult save(@RequestBody SaveReq req) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        CrawlerArticleEntity article = new CrawlerArticleEntity();
        article.setTitle(req.getTitle());
        article.setContent(req.getContent());
        article.setSummary(req.getSummary());
        article.setAuthor("AI".equals(req.getAuthorType()) ? "AI 自动生成" : loginUser.getUsername());
        article.setStatus(0);
        article.setSourceType("AI");
        article.setCreateBy(loginUser.getUserId());
        article.setDeptId(loginUser.getCurrentDeptId());
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        article.setSourceSite("AI Generated");
        article.setTaskId(0L);

        String uuid = java.util.UUID.randomUUID().toString();
        article.setUrl("AI_GENERATED_" + uuid);
        article.setUrlHash("AI_HASH_" + uuid);
        article.setContentHash("AI_CONTENT_" + uuid);
        article.setIsUpdated(0);
        
        articleService.save(article);
        
        return AjaxResult.success("文章已保存，等待审核", article.getId());
    }

    /**
     * 构建生成提示词
     */
    private String buildGeneratePrompt(String topic, String keywords, String style) {
        StringBuilder sb = new StringBuilder();
        sb.append("请根据以下信息生成一篇").append(style != null ? style : "技术教程").append("文章：\n\n");
        sb.append("主题：").append(topic).append("\n");
        if (keywords != null && !keywords.isEmpty()) {
            sb.append("关键词：").append(keywords).append("\n");
        }
        sb.append("\n请生成完整的文章，包含标题、摘要和正文。\n");
        sb.append("输出格式：\n");
        sb.append("【标题】文章标题\n");
        sb.append("【摘要】100-200字的摘要\n");
        sb.append("【正文】完整的文章正文，使用 Markdown 格式，包含适当的标题层级\n");
        return sb.toString();
    }

    /**
     * 解析生成的内容
     */
    private Map<String, String> parseGeneratedContent(String result) {
        Map<String, String> parsed = new HashMap<>();
        
        String title = extractSection(result, "【标题】", "【");
        String summary = extractSection(result, "【摘要】", "【");
        String content = extractSection(result, "【正文】", null);
        
        parsed.put("title", title != null ? title.trim() : "");
        parsed.put("summary", summary != null ? summary.trim() : "");
        parsed.put("content", content != null ? content.trim() : result);
        
        return parsed;
    }

    private String extractSection(String text, String startTag, String endTagPrefix) {
        int start = text.indexOf(startTag);
        if (start == -1) return null;
        start += startTag.length();
        
        if (endTagPrefix == null) {
            return text.substring(start);
        }
        
        int end = text.indexOf(endTagPrefix, start);
        if (end == -1) {
            return text.substring(start);
        }
        return text.substring(start, end);
    }

    // ========== Request DTOs ==========

    @Data
    public static class GenerateReq {
        private String topic;      // 主题
        private String keywords;   // 关键词
        private String style;      // 风格：技术教程/新闻报道/产品评测等
    }

    @Data
    public static class OptimizeReq {
        private String title;
        private String content;
    }

    @Data
    public static class CorrectReq {
        private String title;
        private String content;
        private String correction; // 修改意见
    }

    @Data
    public static class SaveReq {
        private String title;
        private String content;
        private String summary;
        private String authorType; // "SELF" 或 "AI"
    }
}

