package com.minejufe.backupdashboard.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 翼龙面板 API 的原始响应模型（DTO 层）。
 *
 * <p>职责：只描述"面板返回的 JSON 长什么样"，字段名严格对应翼龙官方 API 文档。
 * 业务代码不直接用它，而是由 {@link com.minejufe.backupdashboard.service.BackupService}
 * 转换成前端用的模型，这样以后面板改字段只影响本文件。
 */
public final class PanelDtos {

    private PanelDtos() {
    }

    /** GET /api/client/servers/{id}/backups 的响应外壳 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BackupListResponse(List<BackupData> data) {
    }

    /** 单个备份对象：{"object":"backup","attributes":{...}} */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BackupData(BackupAttributes attributes) {
    }

    /** 备份属性，翼龙返回 snake_case，这里显式映射成 Java 风格字段 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record BackupAttributes(
            String uuid,
            String name,
            @JsonProperty("bytes") Long bytes,
            @JsonProperty("created_at") String createdAt,
            @JsonProperty("completed_at") String completedAt,
            @JsonProperty("is_successful") Boolean isSuccessful,
            @JsonProperty("checksum") String checksum
    ) {
    }
}
