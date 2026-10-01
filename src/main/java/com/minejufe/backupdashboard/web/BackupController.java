package com.minejufe.backupdashboard.web;

import com.minejufe.backupdashboard.dto.BackupView;
import com.minejufe.backupdashboard.service.BackupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 备份相关的 REST 接口 —— 前端页面唯一对话的后端入口。
 *
 * <p>职责：只做"收请求 -> 调 Service -> 返回 JSON"，不写业务逻辑、不直接碰 HTTP 客户端。
 *
 * <p>接口清单：
 * <ul>
 *   <li>{@code GET  /api/panel/status} —— 面板配置与连通状态</li>
 *   <li>{@code GET  /api/backups} —— 备份列表</li>
 *   <li>{@code POST /api/backups} —— 触发一次备份</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    /** 面板配置状态：页面加载时先调它，决定是否提示"未配置" */
    @GetMapping("/panel/status")
    public BackupView.PanelStatus panelStatus() {
        return backupService.status();
    }

    /** 备份列表 */
    @GetMapping("/backups")
    public BackupView.ListResult listBackups() {
        return backupService.listBackups();
    }

    /** 触发备份：面板返回 202 即视为受理成功 */
    @PostMapping("/backups")
    public ResponseEntity<Map<String, Object>> createBackup() {
        backupService.createBackup();
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "accepted", true,
                "serverId", backupService.status().serverId(),
                "message", "备份请求已被翼龙面板受理，打包在面板侧异步进行，稍后刷新列表查看结果"
        ));
    }
}
