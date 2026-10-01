package com.minejufe.backupdashboard.client;

import org.springframework.http.HttpStatus;

/**
 * 调用翼龙面板失败时抛出的统一异常。
 *
 * <p>职责：把"配置缺失"、"面板返回非 2xx"、"网络连不上"三种失败，
 * 统一成带 HTTP 状态码 + 错误码的形式，交给
 * {@link com.minejufe.backupdashboard.web.GlobalExceptionHandler} 变成前端能读的 JSON。
 */
public class PanelException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private PanelException(HttpStatus status, String code, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.code = code;
    }

    /** 面板地址 / API Key / 服务器 ID 还没填 */
    public static PanelException notConfigured(String detail) {
        return new PanelException(HttpStatus.SERVICE_UNAVAILABLE, "PANEL_NOT_CONFIGURED",
                "翼龙面板尚未配置完整：" + detail + "。请在 application.yaml 的 panel.* 中填写，或在页面上直接填写后重试。",
                null);
    }

    /** 面板返回了非 2xx（例如 Key 无效 401、服务器不存在 404、权限不足 403） */
    public static PanelException apiError(HttpStatus status, String detail) {
        return new PanelException(status, "PANEL_API_ERROR", "翼龙面板返回错误：" + detail, null);
    }

    /** 网络层失败：DNS、连接被拒、超时、证书错误等 */
    public static PanelException networkError(String detail, Throwable cause) {
        return new PanelException(HttpStatus.BAD_GATEWAY, "PANEL_UNREACHABLE",
                "无法连接翼龙面板：" + detail, cause);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
