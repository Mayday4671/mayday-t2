package com.mayday.server.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import lombok.extern.slf4j.Slf4j;
import java.nio.file.Paths;

/**
 * Web MVC 配置
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Configuration
@Slf4j
public class WebConfig implements WebMvcConfigurer {

    @Value("${mayday.upload.path}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取绝对路径
        String absolutePath = Paths.get(uploadPath).toAbsolutePath().toUri().toString();
        // 确保以 / 结尾
        if (!absolutePath.endsWith("/")) {
            absolutePath += "/";
        }
        log.info("静态资源映射: /upload/** -> {}", absolutePath);
        
        // 映射 /upload/** 到本地文件系统
        registry.addResourceHandler("/upload/**")
                .addResourceLocations(absolutePath);
    }
}
