package com.minejufe.backupdashboard.web;

import com.minejufe.backupdashboard.client.PanelException;
import com.minejufe.backupdashboard.dto.BackupView.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

/**
 * 全局异常处理 —— 保证前端永远收到统一结构的错误 JSON，而不是 HTML 错误页。
 *
 * <p>职责：
 * <ul>
 *   <li>{@link PanelException} -> 用它自带的状态码与错误码（未配置 503、面板报错原样透传、连不上 502）；</li>
 *   <li>{@link ResourceAccessException} -> 兜底网络异常，转 502；</li>
 *   <li>其他异常 -> 500，同时打日志方便排查。</li>
 * </ul>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PanelException.class)
    public ResponseEntity<ApiError> handlePanel(PanelException e) {
        log.warn("面板调用失败 [{}] {}", e.getCode(), e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(new ApiError(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> handleNetwork(ResourceAccessException e) {
        log.warn("面板网络异常：{}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiError("PANEL_UNREACHABLE", "无法连接翼龙面板：" + e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception e) {
        log.error("服务内部错误", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("INTERNAL_ERROR", "服务内部错误：" + e.getMessage()));
    }
}
