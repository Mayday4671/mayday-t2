package com.mayday.ai.provider;

import com.mayday.ai.factory.AiHttpClientFactory;
import com.mayday.ai.model.entity.AiConfigEntity;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;


/**
 * Google Gemini Provider
 * - 使用 LangChain4j 的 GoogleAiGeminiChatModel
 */
@Component
public class GoogleGeminiProvider implements AiProvider {

    private final AiHttpClientFactory httpFactory;

    public GoogleGeminiProvider(AiHttpClientFactory httpFactory) {
        this.httpFactory = httpFactory;
    }

    @Override
    public String provider() {
        return "google";
    }

    @Override
    public ChatLanguageModel buildChatModel(AiConfigEntity cfg, String apiKeyPlain) {
        double temp = (cfg.getTemperature() == null) ? 0.7 : cfg.getTemperature().doubleValue();

        Proxy proxy = httpFactory.buildProxyOrNull();

        var b = OpenAiChatModel.builder()
            .apiKey(apiKeyPlain)
            .baseUrl(cfg.getBaseUrl())       // ✅ DB 配置
            .modelName(cfg.getModelName())   // ✅ DB 配置
            .temperature(temp);

        // 你的版本支持 .proxy(proxy) 就用，不支持就删掉换全局 JVM proxy（但你现在已经支持了）
        if (proxy != null) {
            b.proxy(proxy);
        }

        return b.build();
    }

    public static void main(String[] args)
    {
        // 【新增】设置代理，端口号 7890 请根据你实际使用的代理软件修改
        System.setProperty("http.proxyHost", "127.0.0.1");
        System.setProperty("http.proxyPort", "7897");
        System.setProperty("https.proxyHost", "127.0.0.1");
        System.setProperty("https.proxyPort", "7897");
        // 定义仅供这个对象使用的代理
        Proxy myProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("127.0.0.1", 7890));

        ChatLanguageModel model = OpenAiChatModel.builder()
            .apiKey("YOUR_GOOGLE_API_KEY") // 这里填 Google 的 Key
            // .baseUrl("https://generativelanguage.googleapis.com/v1beta/openai/") // 关键：Gemini 的 OpenAI 兼容端点
            .modelName("gemini-2.5-flash")
            .proxy(myProxy) // ✅ 终于可以用这个方法了！
            .timeout(Duration.ofSeconds(60))
            .build();

        String response = model.generate("用一句话介绍 Spring Boot");
        System.out.println(response);
    }
}
