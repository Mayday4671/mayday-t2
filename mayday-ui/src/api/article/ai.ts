import request from "../../utils/request";

/**
 * AI 文章生成 API
 */

/**
 * 生成文章
 * @param topic 主题
 * @param keywords 关键词
 * @param style 风格
 */
export function generateArticle(data: { topic: string; keywords?: string; style?: string }) {
    return request.post("/article/ai/generate", data);
}

/**
 * 优化文章
 * @param title 标题
 * @param content 正文
 */
export function optimizeArticle(data: { title: string; content: string }) {
    return request.post("/article/ai/optimize", data);
}

/**
 * 修正文章
 * @param title 标题
 * @param content 正文
 * @param correction 修改意见
 */
export function correctArticle(data: { title: string; content: string; correction: string }) {
    return request.post("/article/ai/correct", data);
}

/**
 * 保存 AI 生成的文章
 * @param title 标题
 * @param content 正文
 * @param summary 摘要
 * @param authorType 作者类型：SELF-自己 AI-AI生成
 */
export function saveAiArticle(data: { title: string; content: string; summary: string; authorType: string }) {
    return request.post("/article/ai/save", data);
}
