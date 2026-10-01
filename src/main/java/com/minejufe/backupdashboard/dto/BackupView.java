package com.minejufe.backupdashboard.dto;

import java.util.List;

/**
 * 前端（页面）使用的数据模型。
 *
 * <p>职责：把翼龙面板的原始 JSON 整理成页面直接能用的形状，
 * 顺便把字节数换算成 KB/MB/GB、把 ISO 时间转成本地可读格式。
 * 这样前端只负责渲染，不需要懂面板的字段细节。
 */
public final class BackupView {

    private BackupView() {
    }

    /** 一条备份记录 */
    public record Item(
            String uuid,
            String name,
            long bytes,
            String sizeText,
            String createdAt,
            String completedAt,
            boolean successful,
            boolean hasChecksum
    ) {
    }

    /** 备份列表响应 */
    public record ListResult(
            String serverId,
            int total,
            List<Item> items
    ) {
    }

    /** 面板配置与连通状态 */
    public record PanelStatus(
            boolean configured,
            String url,
            String serverId,
            String apiKeyMasked,
            String status,
            String message
    ) {
    }

    /** 统一错误响应 */
    public record ApiError(
            String code,
            String message
    ) {
    }

    /** 字节数转可读文本：1536 -> 1.50 KB */
    public static String humanSize(long bytes) {
        if (bytes < 0) {
            return "未知";
        }
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double value = bytes;
        int unitIndex = -1;
        while (value >= 1024 && unitIndex < units.length - 1) {
            value /= 1024;
            unitIndex++;
        }
        return String.format("%.2f %s", value, units[unitIndex]);
    }
}
