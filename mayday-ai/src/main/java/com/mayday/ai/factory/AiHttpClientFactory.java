package com.mayday.ai.factory;

import com.mayday.ai.config.AiNetProperties;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;

@Component
public class AiHttpClientFactory {

    private final AiNetProperties props;

    public AiHttpClientFactory(AiNetProperties props) {
        this.props = props;
    }

    public Proxy buildProxyOrNull() {
        var p = props.getProxy();
        if (p == null || Boolean.FALSE.equals(p.getEnabled())) return null;
        if (p.getHost() == null || p.getPort() == null) return null;

        Proxy.Type type = "SOCKS".equalsIgnoreCase(p.getType()) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
        return new Proxy(type, new InetSocketAddress(p.getHost(), p.getPort()));
    }

    public OkHttpClient buildClient() {
        int timeout = props.getTimeoutSeconds() == null ? 120 : props.getTimeoutSeconds();
        return new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(Math.min(timeout, 300)))
            .readTimeout(Duration.ofSeconds(Math.min(timeout, 300)))
            .writeTimeout(Duration.ofSeconds(Math.min(timeout, 300)))
            .build();
    }
}