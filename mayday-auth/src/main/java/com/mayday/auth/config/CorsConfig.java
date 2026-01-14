package com.mayday.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

/**
 * CORS 跨域配置
 * <p>
 * 允许前端开发服务器 (如 Vite) 跨域访问后端 API。
 * </p>
 *
 * @author MayDay Auth Generator
 * @since 1.0.0
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        
        // 允许的来源 (开发环境)
        config.setAllowedOriginPatterns(Arrays.asList("*"));
        
        // 允许携带 Cookie
        config.setAllowCredentials(true);
        
        // 允许的请求方法
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // 允许的请求头
        config.setAllowedHeaders(Arrays.asList("*"));
        
        // 暴露的响应头
        config.setExposedHeaders(Arrays.asList("Authorization"));
        
        // 预检请求有效期 (秒)
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return new CorsFilter(source);
    }
}
