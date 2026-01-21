package com.mayday.server.controller;

import com.mayday.ai.api.AiService;
import com.mayday.auth.model.LoginUser;
import com.mayday.auth.util.SecurityUtils;
import com.mayday.common.web.AjaxResult;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * AI 文章生成控制器
 *
 * @author Antigravity
 * @since 1.0.0
 */
@RestController
@RequestMapping("/article/ai")
@RequiredArgsConstructor
public class ArticleAiController {

    private final AiService aiService;
    private final ICrawlerArticleService articleService;

    /**
     * 生成文章
     * 
     * @param req 生成请求（主题、关键词、风格）
     * @return 生成的文章内容（标题、正文、摘要）
     */
    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('article:ai:generate')")
    public AjaxResult generate(@RequestBody GenerateReq req) {
        // 构建生成提示词
        String prompt = buildGeneratePrompt(req.getTopic(), req.getKeywords(), req.getStyle());
        
        try {
            // 调用 AI 服务生成内容
            String result = aiService.chat("article_generation", "*", prompt);
            
            // 解析生成结果
            Map<String, String> parsed = parseGeneratedContent(result);
            
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    /**
     * 优化文章
     */
    @PostMapping("/optimize")
    @PreAuthorize("hasAuthority('article:ai:generate')")
    public AjaxResult optimize(@RequestBody OptimizeReq req) {
        String prompt = "请优化以下文章内容，使其更加专业、流畅、有吸引力：\n\n" +
                "标题：" + req.getTitle() + "\n\n" +
                "正文：" + req.getContent() + "\n\n" +
                "请保持技术教程风格，输出格式：\n【标题】xxx\n【摘要】xxx\n【正文】xxx";
        
        try {
            String result = aiService.chat("article_generation", "*", prompt);
            Map<String, String> parsed = parseGeneratedContent(result);
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
    }

    /**
     * 修正文章
     */
    @PostMapping("/correct")
    @PreAuthorize("hasAuthority('article:ai:generate')")
    public AjaxResult correct(@RequestBody CorrectReq req) {
        String prompt = "请根据以下修改意见修正文章：\n\n" +
                "原标题：" + req.getTitle() + "\n" +
                "原正文：" + req.getContent() + "\n\n" +
                "修改意见：" + req.getCorrection() + "\n\n" +
                "请输出修正后的内容，格式：\n【标题】xxx\n【摘要】xxx\n【正文】xxx";
        
        try {
            String result = aiService.chat("article_generation", "*", prompt);
            Map<String, String> parsed = parseGeneratedContent(result);
            return AjaxResult.success(parsed);
        } catch (Exception e) {
            return handleAiException(e);
        }
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
    @PreAuthorize("hasAuthority('article:ai:generate')")
    public AjaxResult save(@RequestBody SaveReq req) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        
        CrawlerArticleEntity article = new CrawlerArticleEntity();
        article.setTitle(req.getTitle());
        article.setContent(req.getContent());
        article.setSummary(req.getSummary());
        
        // 作者：用户选择"自己"或"AI 自动生成"
        article.setAuthor("AI".equals(req.getAuthorType()) ? "AI 自动生成" : loginUser.getUsername());
        
        // 审核状态：待审核
        article.setStatus(0);
        
        // 来源类型：AI 生成
        article.setSourceType("AI");
        
        // 设置创建人和部门
        article.setCreateBy(loginUser.getUserId());
        article.setDeptId(loginUser.getCurrentDeptId());
        article.setCreateTime(new Date());
        article.setUpdateTime(new Date());
        
        // 来源站点标记
        article.setSourceSite("AI Generated");
        
        // 设置默认任务ID，防止数据库报错
        article.setTaskId(0L);

        // 设置虚拟 URL 和 Hash，防止数据库报错
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
        
        // 简单解析，提取标题、摘要、正文
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
