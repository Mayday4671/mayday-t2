package com.mayday.crawler.util;

import org.jsoup.Connection;

import java.util.HashMap;
import java.util.Map;

/**
 * 请求头构建工具，用于统一设置 Jsoup 请求头，模拟真实浏览器访问。
 *
 * 这是一个精简版实现，只包含当前项目用到的能力：
 * - create()：创建构建器
 * - asImageRequest()：按图片请求的常用头部初始化
 * - withReferer()：设置 Referer 头
 * - applyTo()：把头部应用到 Jsoup Connection 上
 */
public class RequestHeaderBuilder
{
    private final Map<String, String> headers = new HashMap<>();

    private RequestHeaderBuilder()
    {
    }

    public static RequestHeaderBuilder create()
    {
        return new RequestHeaderBuilder();
    }

    /**
     * 按图片请求场景初始化常用请求头（User-Agent / Accept 等）。
     */
    public RequestHeaderBuilder asImageRequest()
    {
        // 常见浏览器 UA
        headers.put("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // 图片相关 Accept
        headers.put("Accept",
                "image/avif,image/webp,image/apng,image/*,*/*;q=0.8");

        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");

        return this;
    }

    /**
     * 设置 Referer。
     */
    public RequestHeaderBuilder withReferer(String referer)
    {
        if (referer != null && !referer.isEmpty())
        {
            headers.put("Referer", referer);
        }
        return this;
    }

    /**
     * 允许按需追加自定义请求头。
     */
    public RequestHeaderBuilder addHeader(String name, String value)
    {
        if (name != null && !name.isEmpty() && value != null)
        {
            headers.put(name, value);
        }
        return this;
    }

    /**
     * 将构建好的请求头应用到 Jsoup Connection 上。
     */
    public void applyTo(Connection connection)
    {
        if (connection == null || headers.isEmpty())
        {
            return;
        }
        for (Map.Entry<String, String> entry : headers.entrySet())
        {
            connection.header(entry.getKey(), entry.getValue());
        }
    }
}

