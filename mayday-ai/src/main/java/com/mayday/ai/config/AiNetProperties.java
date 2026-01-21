package com.mayday.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Data
@ConfigurationProperties(prefix = "mayday.ai.net")
public class AiNetProperties {

    private Integer timeoutSeconds = 120;
    private Proxy proxy = new Proxy();

    @Data
    public static class Proxy {
        private Boolean enabled = false;
        private String type = "HTTP"; // HTTP / SOCKS
        private String host;
        private Integer port;
    }
}