package com.mayday.server.swagger;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.net.InetAddress;


/**
 * 启动完成后打印常用访问地址（Swagger/Knife4j）
 *
 * 说明：
 * - 触发时机：应用完全启动完（Tomcat 已监听端口）
 * - 自动读取 yml 配置：server.port / swagger-ui.path / api-docs.path
 */
@Slf4j
@Component
public class StartupInfoLogger {

    @Value("${server.port:8080}")
    private int port;

    @Value("${server.address:}")
    private String address;

    @Value("${springdoc.swagger-ui.path:/swagger-ui.html}")
    private String swaggerUiPath;

    @Value("${springdoc.api-docs.path:/v3/api-docs}")
    private String apiDocsPath;

    @Value("${knife4j.enable:false}")
    private boolean knife4jEnabled;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        String host = resolveHost();

        String base = "http://" + host + ":" + port;

        // 你配置的 swagger-ui.path 是 /swagger-ui.html
        String swagger = base + swaggerUiPath;

        // /v3/api-docs
        String apiDocs = base + apiDocsPath;

        log.info("============================================================");
        log.info("Application is ready ✅");
        log.info("Swagger UI  : {}", swagger);
        log.info("OpenAPI JSON: {}", apiDocs);

        if (knife4jEnabled) {
            // knife4j 默认也能从 swagger-ui 入口进；有的项目是 /doc.html（按你依赖/版本）
            log.info("Knife4j     : {}{}", base, "/doc.html");
        }

        log.info("============================================================");
    }

    private String resolveHost() {
        try {
            if (address != null && !address.isBlank()) return address;
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}