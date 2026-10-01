package com.minejufe.backupdashboard.client;

import com.minejufe.backupdashboard.client.PanelDtos.BackupListResponse;
import com.minejufe.backupdashboard.config.PanelProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Duration;

/**
 * 翼龙面板(Pterodactyl) API 客户端 —— 唯一直接与面板 HTTP 通信的类。
 *
 * <p>职责：
 * <ol>
 *   <li>按 {@link PanelProperties} 拼出请求地址与鉴权头（Client API：{@code Authorization: Bearer <key>}）；</li>
 *   <li>封装"列备份"与"新建备份"两个面板接口；</li>
 *   <li>把面板的 4xx/5xx 与网络异常翻译成 {@link PanelException}，让上层只面对一种错误类型。</li>
 * </ol>
 *
 * <p>用到的翼龙 Client API（v1）：
 * <ul>
 *   <li>{@code GET  /api/client/servers/{server}/backups} —— 备份列表</li>
 *   <li>{@code POST /api/client/servers/{server}/backups} —— 创建备份，body 传 {@code {}}</li>
 * </ul>
 *
 * <p>为方便自建/自签证书的面板，这里默认信任所有证书；后面要上生产就把
 * {@link #TRUST_ALL_CERTIFICATES} 改成 {@code false}，走标准证书校验。
 */
@Component
public class PterodactylPanelClient {

    /** 是否跳过 TLS 证书校验（自签名面板用） */
    private static final boolean TRUST_ALL_CERTIFICATES = true;

    private final PanelProperties properties;

    public PterodactylPanelClient(PanelProperties properties) {
        this.properties = properties;
    }

    /** 拉取指定服务器的全部备份 */
    public BackupListResponse listBackups() {
        return buildClient()
                .get()
                .uri("/api/client/servers/{server}/backups", properties.getServerId().trim())
                .retrieve()
                .body(BackupListResponse.class);
    }

    /** 触发一次备份；翼龙会立即返回 202，实际打包在面板侧异步进行 */
    public void createBackup() {
        buildClient()
                .post()
                .uri("/api/client/servers/{server}/backups", properties.getServerId().trim())
                .contentType(MediaType.APPLICATION_JSON)
                .body("{}")
                .retrieve()
                .toBodilessEntity();
    }

    /**
     * 依当前配置构造 RestClient。每次调用都重新构造，这样页面上改完配置
     * 不需要重启应用就能生效。
     */
    private RestClient buildClient() {
        requireConfigured();

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(Math.max(1, properties.getConnectTimeoutSeconds())))
                .withReadTimeout(Duration.ofSeconds(Math.max(1, properties.getReadTimeoutSeconds())));

        ClientHttpRequestFactoryBuilder<?> builder;
        if (TRUST_ALL_CERTIFICATES) {
            SSLContext sslContext = trustAllSslContext();
            builder = ClientHttpRequestFactoryBuilder.jdk()
                    .withHttpClientCustomizer(httpClient -> httpClient.sslContext(sslContext));
        } else {
            builder = ClientHttpRequestFactoryBuilder.jdk();
        }
        ClientHttpRequestFactory requestFactory = builder.build(settings);

        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey().trim())
                .defaultHeader("Accept", "application/json")
                .defaultStatusHandler(
                        statusCode -> statusCode.isError(),
                        (request, response) -> {
                            throw PanelException.apiError(
                                    HttpStatus.valueOf(response.getStatusCode().value()),
                                    describe(response));
                        })
                .build();
    }

    private void requireConfigured() {
        if (!properties.isConfigured()) {
            StringBuilder missing = new StringBuilder();
            if (isBlank(properties.getUrl())) {
                missing.append("面板地址 ");
            }
            if (isBlank(properties.getApiKey())) {
                missing.append("API Key ");
            }
            if (isBlank(properties.getServerId())) {
                missing.append("服务器 ID ");
            }
            throw PanelException.notConfigured(missing.toString().trim());
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** 尽量把面板返回的错误正文（翼龙是 {"errors":[{"detail":"..."}]}）转成一句人话 */
    private String describe(ClientHttpResponse response) {
        try {
            String statusText = "HTTP " + response.getStatusCode().value();
            String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
            return body.isBlank() ? statusText : statusText + " - " + firstDetail(body);
        } catch (Exception e) {
            // 状态码或正文读不出来时，退化成异常信息
            return "响应解析失败：" + e.getMessage();
        }
    }

    private String firstDetail(String body) {
        int keyIndex = body.indexOf("\"detail\"");
        if (keyIndex < 0) {
            return body.strip();
        }
        int colon = body.indexOf(':', keyIndex);
        if (colon < 0) {
            return body.strip();
        }
        int firstQuote = body.indexOf('"', colon + 1);
        if (firstQuote < 0) {
            return body.strip();
        }
        int secondQuote = body.indexOf('"', firstQuote + 1);
        if (secondQuote < 0) {
            return body.strip();
        }
        return body.substring(firstQuote + 1, secondQuote);
    }

    /** 自签名证书面板用：信任所有证书 */
    private SSLContext trustAllSslContext() {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return new X509Certificate[0];
                }
            }}, new SecureRandom());
            return context;
        } catch (Exception e) {
            throw new IllegalStateException("构造信任所有证书的 SSLContext 失败", e);
        }
    }
}
