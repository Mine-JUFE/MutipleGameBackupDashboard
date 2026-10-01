# MutipleGameBackupDashboard

多功能游戏数据备份面板。当前版本已完成 **翼龙面板 (Pterodactyl) API 接入** 的最小可用骨架：
读取面板备份列表 + 通过 API 触发一次服务器备份。

后续规划：Onedrive / Google Drive / R1 / WebDAV 等多种上传协议、定时自动备份、多服务器统一管理。

---

## 技术栈

| 项目 | 版本 / 说明 |
| --- | --- |
| 语言 | Java 21 语法，本机 JDK 25 编译 |
| 框架 | Spring Boot 3.5.16（spring-boot-starter-web，内嵌 Tomcat） |
| 构建 | Gradle（已带 Wrapper，锁定 9.2.1） |
| 前端 | 单文件原生 HTML/CSS/JS（放在 `static/`，由 Spring Boot 直接托管） |
| 面板通信 | Spring `RestClient` + JDK `HttpClient` |

## 快速开始

1. 填写面板信息：编辑 `src/main/resources/application.yml`

   ```yaml
   panel:
     url: "https://panel.example.com"   # 面板根地址，结尾不要带斜杠
     api-key: "ptlc_xxxxxxxx"           # 面板 账户 -> API Credentials 生成
     server-id: "3f1a2b4c"              # 面板 /server/<id> 里的那串 ID
   ```

2. 启动：

   ```bash
   ./gradlew.bat bootRun          # Windows
   ./gradlew bootRun              # Linux / macOS
   ```

3. 打开 <http://localhost:8080/>，页面会显示面板连接状态和备份列表；
   点「立即备份」即通过翼龙 API 触发一次备份。

### 不接真实面板也能联调

内置了一个假翼龙面板，用于在没有面板/密钥时验证整条链路：

```bash
node devtools/mock-panel.js                                  # 终端 1：假面板，监听 9099
./gradlew.bat bootRun --args='--spring.profiles.active=mock'  # 终端 2：应用指向假面板
```

假面板会把收到的每个请求（方法、路径、Authorization 头、body）打印出来，
可以直观确认我们发出的请求符合翼龙 API 规范。

## HTTP 接口

| 方法 | 路径 | 作用 |
| --- | --- | --- |
| GET | `/api/panel/status` | 面板配置状态（地址、服务器 ID、脱敏 Key） |
| GET | `/api/backups` | 备份列表（已换算体积、格式化时间、按时间倒序） |
| POST | `/api/backups` | 触发一次备份，成功返回 202 |

错误响应统一为 `{"code": "...", "message": "..."}`：

| code | HTTP | 场景 |
| --- | --- | --- |
| `PANEL_NOT_CONFIGURED` | 503 | 面板地址 / API Key / 服务器 ID 未填写 |
| `PANEL_API_ERROR` | 面板原状态码（401/403/404…） | 面板返回非 2xx，message 内透传面板的 detail |
| `PANEL_UNREACHABLE` | 502 | DNS、连接被拒、超时、证书等网络问题 |
| `INTERNAL_ERROR` | 500 | 其他服务端异常 |

## 目录结构与文件职责

```
MutipleGameBackupDashboard/
├── build.gradle                    构建脚本：依赖、Java 版本、镜像仓库、Wrapper 版本
├── settings.gradle                 工程名
├── gradle/wrapper/                 Gradle Wrapper（锁定 9.2.1）
├── devtools/
│   └── mock-panel.js               开发用假翼龙面板（Node，零依赖），用于本地联调
└── src/main/
    ├── java/com/minejufe/backupdashboard/
    │   ├── BackupDashboardApplication.java   启动类
    │   ├── config/PanelProperties.java       面板配置绑定（panel.*）
    │   ├── client/PanelDtos.java             面板原始 JSON 模型
    │   ├── client/PterodactylPanelClient.java 翼龙 API 客户端（唯一发 HTTP 的类）
    │   ├── client/PanelException.java        面板调用异常
    │   ├── service/BackupService.java        备份业务逻辑 + 数据转换
    │   ├── dto/BackupView.java               页面用数据模型
    │   ├── web/BackupController.java         REST 接口
    │   └── web/GlobalExceptionHandler.java   全局异常 -> 统一 JSON
    └── resources/
        ├── application.yml                  主配置（面板密钥填这里）
        ├── application-mock.yml             联调用的配置（指向假面板）
        └── static/index.html                前端单页面板
```

### 各文件 / 类的作用

| 文件 | 类 | 职责 |
| --- | --- | --- |
| `BackupDashboardApplication.java` | `BackupDashboardApplication` | 程序入口。启动 Spring Boot、扫描 Bean、绑定 `panel.*` 配置。 |
| `config/PanelProperties.java` | `PanelProperties` | **只描述配置结构**。保存面板地址、API Key、服务器 ID、超时；提供 `baseUrl()` 去尾斜杠、`maskedApiKey()` 脱敏、`isConfigured()` 判断是否配置完整。 |
| `client/PanelDtos.java` | `PanelDtos` 及内部 record | **只描述面板返回的 JSON**。字段对应翼龙 API 文档的 snake_case（`created_at`、`is_successful` 等），面板改字段只影响这个文件。 |
| `client/PterodactylPanelClient.java` | `PterodactylPanelClient` | **唯一直接与面板通信的类**。拼 URL、加 `Authorization: Bearer` 头、封装"列备份 / 建备份"两个接口，并把面板 4xx/5xx 翻译成 `PanelException`。也可在此切换为信任自签名证书。 |
| `client/PanelException.java` | `PanelException` | 面板调用的统一异常，携带 HTTP 状态码 + 业务错误码（未配置 / 面板报错 / 连不上）。 |
| `service/BackupService.java` | `BackupService` | **业务层**。暴露 `listBackups()`、`createBackup()`、`status()`；把面板 DTO 转成页面模型（字节换 MB/GB、ISO 时间转本地时间、按创建时间倒序）；把网络异常包装成 `PanelException`；打操作日志。后续定时备份、上传网盘都在这一层扩展。 |
| `dto/BackupView.java` | `Item` / `ListResult` / `PanelStatus` / `ApiError` | **前端要看到的数据形状**。前端只渲染，不需要懂面板字段。含 `humanSize()` 体积换算工具。 |
| `web/BackupController.java` | `BackupController` | **REST 入口**。只做"收请求 -> 调 Service -> 返 JSON"，不写业务逻辑。 |
| `web/GlobalExceptionHandler.java` | `GlobalExceptionHandler` | 把异常统一转成 `{"code","message"}` JSON，避免前端收到 HTML 错误页。 |
| `resources/static/index.html` | — | 前端单页面板：顶部状态徽标 + 刷新/备份按钮，中部面板连接信息，下部备份记录表格与接入说明。 |

## 配置说明

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `server.port` | 8080 | 面板 Web 端口 |
| `panel.url` | 空 | 翼龙面板根地址，结尾不要带 `/` |
| `panel.api-key` | 空 | Client API Key（账户 → API Credentials），权限需含 Backups 读写 |
| `panel.server-id` | 空 | 目标游戏服务器短 ID |
| `panel.connect-timeout-seconds` | 10 | 连接面板超时 |
| `panel.read-timeout-seconds` | 30 | 读取响应超时 |

## 安全提醒

- `PterodactylPanelClient.TRUST_ALL_CERTIFICATES` 目前为 `true`，用于兼容自签名证书的面板；
  接公网正式面板时请改为 `false`，走标准证书校验。
- API Key 属于敏感信息，请勿提交到 Git（`.gitignore` 已忽略 `application-local.*`）。
  建议把密钥写在 `application-local.yml`，用 `--spring.profiles.active=local` 启动。

## 已知限制 / 下一步

- [ ] 页面上的配置目前只读，修改需要在 `application.yml` 中改后重启
- [ ] 尚未实现备份删除 / 下载 / 还原
- [ ] 尚未实现定时自动备份
- [ ] 尚未实现 Onedrive / Google Drive / R1 / WebDAV 上传
- [ ] 尚未实现多服务器、多面板统一管理
- [ ] 无单元测试（`gradle test` 目前为 NO-SOURCE）
