package com.minejufe.backupdashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 翼龙面板(Pterodactyl) 连接配置。
 *
 * <p>职责：只负责"配置长什么样"，不含任何业务逻辑。字段与 application.yaml 中的
 * {@code panel.*} 一一对应，由 Spring Boot 在启动时绑定。
 *
 * <pre>
 * panel:
 *   url: https://panel.example.com      # 面板根地址，不要带结尾斜杠
 *   api-key: ptlc_xxxxxxxxxxxx          # Client API Key（Account -> API Credentials）
 *   server-id: 3f1a2b4c                  # 要操作的游戏服务器短 ID
 *   connect-timeout-seconds: 10          # 连接超时
 *   read-timeout-seconds: 30             # 读取超时
 * </pre>
 */
@ConfigurationProperties(prefix = "panel")
public class PanelProperties {

    /** 面板根地址，例如 https://panel.example.com */
    private String url = "";

    /** Client API Key（在面板"账户 -> API 凭据"中生成，权限选 Backup 相关） */
    private String apiKey = "";

    /** 目标游戏服务器的短 ID（面板服务器地址栏 /server/<id> 里的那串） */
    private String serverId = "";

    /** 连接面板的超时时间（秒） */
    private int connectTimeoutSeconds = 10;

    /** 等待面板响应的超时时间（秒） */
    private int readTimeoutSeconds = 30;

    public boolean isConfigured() {
        return !url.isBlank() && !apiKey.isBlank() && !serverId.isBlank();
    }

    /** 去掉结尾斜杠，避免拼出 https://panel//api/... 这种地址 */
    public String baseUrl() {
        String trimmed = url == null ? "" : url.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    /** 只暴露尾部 4 位，用于前端/日志显示，避免泄露完整密钥 */
    public String maskedApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        String tail = apiKey.length() <= 4 ? apiKey : apiKey.substring(apiKey.length() - 4);
        return "****" + tail;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    public void setConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
    }

    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }

    public void setReadTimeoutSeconds(int readTimeoutSeconds) {
        this.readTimeoutSeconds = readTimeoutSeconds;
    }
}
