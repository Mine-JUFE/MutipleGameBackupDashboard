package com.minejufe.backupdashboard.service;

import com.minejufe.backupdashboard.client.PanelDtos.BackupAttributes;
import com.minejufe.backupdashboard.client.PanelDtos.BackupData;
import com.minejufe.backupdashboard.client.PanelDtos.BackupListResponse;
import com.minejufe.backupdashboard.client.PanelException;
import com.minejufe.backupdashboard.client.PterodactylPanelClient;
import com.minejufe.backupdashboard.config.PanelProperties;
import com.minejufe.backupdashboard.dto.BackupView;
import com.minejufe.backupdashboard.dto.BackupView.Item;
import com.minejufe.backupdashboard.dto.BackupView.ListResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 备份业务服务 —— 面板数据与页面数据之间的转换层，也是页面调用的业务入口。
 *
 * <p>职责：
 * <ol>
 *   <li>对外提供两个业务动作：{@link #listBackups()} 查列表、{@link #createBackup()} 建备份；</li>
 *   <li>把面板 DTO 转成 {@link BackupView}（换算体积、格式化时间、按时间倒序）；</li>
 *   <li>把网络层异常（连不上/超时）包装成 {@link PanelException}，保证错误口径一致；</li>
 *   <li>记录操作日志。后续要加"定时备份""备份到网盘"都在这一层扩展。</li>
 * </ol>
 */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PterodactylPanelClient panelClient;
    private final PanelProperties properties;

    public BackupService(PterodactylPanelClient panelClient, PanelProperties properties) {
        this.panelClient = panelClient;
        this.properties = properties;
    }

    /** 查询目标服务器的所有备份，按创建时间从新到旧 */
    public ListResult listBackups() {
        log.info("查询面板备份列表：panel={} server={}", properties.baseUrl(), properties.getServerId());

        BackupListResponse response;
        try {
            response = panelClient.listBackups();
        } catch (ResourceAccessException e) {
            throw PanelException.networkError(rootMessage(e), e);
        }

        List<Item> items = new ArrayList<>();
        if (response != null && response.data() != null) {
            for (BackupData data : response.data()) {
                BackupAttributes attributes = data == null ? null : data.attributes();
                if (attributes == null) {
                    continue;
                }
                long bytes = attributes.bytes() == null ? -1L : attributes.bytes();
                items.add(new Item(
                        attributes.uuid(),
                        attributes.name(),
                        bytes,
                        bytes < 0 ? "未知" : BackupView.humanSize(bytes),
                        format(attributes.createdAt()),
                        format(attributes.completedAt()),
                        Boolean.TRUE.equals(attributes.isSuccessful()),
                        attributes.checksum() != null && !attributes.checksum().isBlank()
                ));
            }
        }

        items.sort(Comparator.comparing(Item::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return new ListResult(properties.getServerId(), items.size(), items);
    }

    /** 触发一次备份（面板异步执行打包） */
    public void createBackup() {
        log.info("触发服务器备份：panel={} server={}", properties.baseUrl(), properties.getServerId());
        try {
            panelClient.createBackup();
        } catch (ResourceAccessException e) {
            throw PanelException.networkError(rootMessage(e), e);
        }
        log.info("备份请求已被面板接受：server={}", properties.getServerId());
    }

    /** 面板连接配置状态，供页面顶部显示 */
    public BackupView.PanelStatus status() {
        boolean configured = properties.isConfigured();
        return new BackupView.PanelStatus(
                configured,
                properties.baseUrl(),
                properties.getServerId(),
                properties.maskedApiKey(),
                configured ? "CONFIGURED" : "UNCONFIGURED",
                configured ? "配置已就绪，可执行备份操作" : "请在 application.yaml 的 panel.* 中填写面板地址、API Key、服务器 ID"
        );
    }

    /** 把 ISO-8601 时间转成本地时区的可读文本；解析不了就原样返回 */
    private String format(String isoTime) {
        if (isoTime == null || isoTime.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(isoTime)
                    .atZoneSameInstant(java.time.ZoneId.systemDefault())
                    .format(DISPLAY_FORMAT);
        } catch (Exception ignored) {
            return isoTime;
        }
    }

    private String rootMessage(Throwable e) {
        Throwable current = e;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        String message = current.getMessage();
        return message == null ? current.getClass().getSimpleName() : message;
    }
}
