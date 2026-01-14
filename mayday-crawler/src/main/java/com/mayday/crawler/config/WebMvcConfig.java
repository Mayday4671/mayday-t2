package com.mayday.crawler.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * Web MVC配置类
 * 用于配置静态资源访问
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer
{
    /**
     * 图片存储根目录（兼容两种配置键）
     * - crawler.image.base-path
     * - crawler.image-base-path
     */
    @Value("${crawler.image.base-path:${crawler.image-base-path:./data/crawler-images}}")
    private String imageBasePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        // 配置爬虫图片静态资源访问路径
        // 注意：由于 context-path 是 /api，所以实际访问路径是 /api/crawler-images/**
        // 但这里配置的是相对于 context-path 的路径，所以使用 /crawler-images/**
        String absolutePath = Paths.get(imageBasePath).toAbsolutePath().normalize().toString();
        // Windows路径需要转换为URL格式（将反斜杠转换为正斜杠）
        String urlPath = absolutePath.replace("\\", "/");
        // 确保路径以 / 结尾
        if (!urlPath.endsWith("/"))
        {
            urlPath += "/";
        }
        
        System.out.println("====== [WebMvcConfig] 配置静态资源映射 ======");
        System.out.println("映射路径: /crawler-images/**");
        System.out.println("物理路径: file:" + urlPath);
        System.out.println("==========================================");
        
        registry.addResourceHandler("/crawler-images/**")
                .addResourceLocations("file:" + urlPath);
    }
}
