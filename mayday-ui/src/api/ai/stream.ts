/**
 * AI 流式 API
 * 使用 EventSource/fetch 实现流式调用
 *
 * @author Antigravity
 * @since 1.0.0
 */

const BASE_URL = 'http://localhost:9002';

/**
 * 通用流式请求函数
 */
async function streamRequest(
    url: string,
    params: any,
    method: 'GET' | 'POST',
    onToken: (token: string) => void,
    onError?: (error: string) => void,
    signal?: AbortSignal
): Promise<void> {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('token');

    try {
        let fullUrl = url;
        let body = undefined;

        if (method === 'GET') {
            const query = new URLSearchParams(params).toString();
            fullUrl = `${url}?${query}`;
        } else {
            body = JSON.stringify(params);
        }

        const response = await fetch(fullUrl, {
            method,
            headers: {
                'Accept': 'text/event-stream',
                'Content-Type': 'application/json',
                ...(token ? { 'Authorization': `Bearer ${token}` } : {})
            },
            body,
            signal
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        }

        const reader = response.body?.getReader();
        if (!reader) {
            throw new Error('Response body is not readable');
        }

        const decoder = new TextDecoder();

        while (true) {
            const { done, value } = await reader.read();
            if (done) break;

            const chunk = decoder.decode(value, { stream: true });

            // 解析 SSE 格式
            const lines = chunk.split('\n');
            for (const line of lines) {
                if (line.startsWith('data:')) {
                    // 仅移除 'data:' 前缀，保留内容中的空格（如果协议有空格后缀可以再处理，但通常只需 slice(5)）
                    // 某些 SSE 实现可能会在 data: 后加一个空格，这里做一个安全判断
                    let data = line.slice(5);
                    if (data.startsWith(' ')) {
                        data = data.slice(1);
                    }

                    if (data.trim() === '[DONE]') {
                        return;
                    }

                    try {
                        const parsed = JSON.parse(data);
                        if (parsed && typeof parsed === 'object' && parsed.content) {
                            onToken(parsed.content);
                        } else {
                            // 兼容非 JSON 格式
                            onToken(data);
                        }
                    } catch (e) {
                        // 不是 JSON，直接返回文本
                        onToken(data);
                    }
                } else if (line.startsWith('event:error')) {
                    // 下一行是错误数据
                    continue; // 简单处理，实际错误信息通常在 data 中
                }
            }
        }
    } catch (error: any) {
        if (error.name === 'AbortError') {
            return;
        }
        onError?.(error.message || '流式请求失败');
        throw error;
    }
}

/**
 *流式聊天
 */
export function streamChat(
    prompt: string,
    sceneCode: string = 'chat',
    onToken: (token: string) => void,
    onError?: (error: string) => void
): { promise: Promise<void>; abort: () => void } {
    const controller = new AbortController();
    const promise = streamRequest(
        `${BASE_URL}/ai/chat/stream`,
        { prompt, sceneCode },
        'GET',
        onToken,
        onError,
        controller.signal
    );
    return { promise, abort: () => controller.abort() };
}

/**
 * 文章生成（流式）
 */
export function streamGenerateArticle(
    params: { topic: string; keywords?: string; style?: string },
    onToken: (token: string) => void,
    onError?: (error: string) => void
): { promise: Promise<void>; abort: () => void } {
    const controller = new AbortController();
    const promise = streamRequest(
        `${BASE_URL}/article/ai/generate/stream`,
        params,
        'GET',
        onToken,
        onError,
        controller.signal
    );
    return { promise, abort: () => controller.abort() };
}

/**
 * 文章优化（流式）
 */
export function streamOptimizeArticle(
    params: { title: string; content: string },
    onToken: (token: string) => void,
    onError?: (error: string) => void
): { promise: Promise<void>; abort: () => void } {
    const controller = new AbortController();
    const promise = streamRequest(
        `${BASE_URL}/article/ai/optimize/stream`,
        params,
        'POST',
        onToken,
        onError,
        controller.signal
    );
    return { promise, abort: () => controller.abort() };
}

/**
 * 文章修正（流式）
 */
export function streamCorrectArticle(
    params: { title: string; content: string; correction: string },
    onToken: (token: string) => void,
    onError?: (error: string) => void
): { promise: Promise<void>; abort: () => void } {
    const controller = new AbortController();
    const promise = streamRequest(
        `${BASE_URL}/article/ai/correct/stream`,
        params,
        'POST',
        onToken,
        onError,
        controller.signal
    );
    return { promise, abort: () => controller.abort() };
}
