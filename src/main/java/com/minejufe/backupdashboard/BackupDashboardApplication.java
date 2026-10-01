package com.minejufe.backupdashboard;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 应用入口。
 *
 * <p>职责：启动 Spring Boot（内嵌 Tomcat）、扫描本包及子包下的 Bean，
 * 并把 application.yaml 里 {@code panel.*} 的配置绑定到配置类上。
 * 启动后访问 http://localhost:8080/ 即为前端面板页面。
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BackupDashboardApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackupDashboardApplication.class, args);
    }
}
