package org.jeecg.config.flyway;

import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;

import javax.sql.DataSource;
import java.util.Map;

/**
 * 微服务版数据库自动升级。
 *
 * <p>单体版（jeecg-system-start）的 {@code org.jeecg.config.flyway.FlywayConfig} 不在微服务的
 * classpath 上，且 FlywayAutoConfiguration 在 nacos 的 jeecg.yaml 中已被排除，
 * 导致微服务模式下没有任何代码执行迁移脚本。本类补齐这一能力，使 system 服务启动时自动迁移。
 *
 * <p><b>开关为什么用 jeecg.flyway.enabled 而不是 spring.flyway.enabled：</b>
 * nacos 的 jeecg-dev.yaml 里 {@code spring.flyway.enabled: false} 是为了关闭 Spring Boot 自带的
 * Flyway 自动装配（jeecg.yaml 里同时把 FlywayAutoConfiguration 排除了）。如果这里沿用同名属性，
 * 该值会经 nacos 下发并覆盖本地配置，迁移始终不会执行。因此开关独立命名，默认开启；
 * 需要临时关闭时在 nacos 或本地配置 {@code jeecg.flyway.enabled: false} 即可。
 *
 * <p>其余连接参数复用 nacos 中已有的 spring.flyway.* 配置（locations / encoding 等）。
 *
 * @author jeecg
 */
@Slf4j
@Lazy(false)
@Configuration
public class FlywayConfig {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Environment environment;

    /**
     * 是否开启迁移。默认 true，与单体版的默认策略相反（单体版默认 false）。
     */
    @Value("${jeecg.flyway.enabled:true}")
    private Boolean enabled;

    /**
     * 迁移sql脚本文件存放路径。nacos 中 jeecg-dev.yaml 已配置该值，此处仅作兜底。
     */
    @Value("${spring.flyway.locations:classpath:flyway/sql/mysql}")
    private String locations;

    @Value("${spring.flyway.encoding:UTF-8}")
    private String encoding;

    @Value("${spring.flyway.sql-migration-prefix:V}")
    private String sqlMigrationPrefix;

    @Value("${spring.flyway.sql-migration-separator:__}")
    private String sqlMigrationSeparator;

    @Value("${spring.flyway.placeholder-prefix:#(}")
    private String placeholderPrefix;

    @Value("${spring.flyway.placeholder-suffix:)}")
    private String placeholderSuffix;

    @Value("${spring.flyway.sql-migration-suffixes:.sql}")
    private String sqlMigrationSuffixes;

    @Value("${spring.flyway.validate-on-migrate:true}")
    private Boolean validateOnMigrate;

    @Value("${spring.flyway.baseline-on-migrate:true}")
    private Boolean baselineOnMigrate;

    @Value("${spring.flyway.clean-disabled:true}")
    private Boolean cleanDisabled;

    @PostConstruct
    public void migrate() {
        if (!Boolean.TRUE.equals(enabled)) {
            log.info("【数据库升级】微服务 Flyway 未开启（jeecg.flyway.enabled=false），跳过迁移。");
            return;
        }

        // 只对主数据源执行迁移。动态数据源在此处已初始化完毕。
        DynamicRoutingDataSource ds = (DynamicRoutingDataSource) dataSource;
        Map<String, DataSource> dataSources = ds.getDataSources();
        if (!dataSources.containsKey("master")) {
            log.warn("【数据库升级】未找到 master 数据源，跳过迁移。当前数据源: {}", dataSources.keySet());
            return;
        }

        DataSource master = dataSources.get("master");
        String url = environment.getProperty("spring.datasource.dynamic.datasource.master.url");
        if (url == null || !url.contains("mysql")) {
            log.warn("【数据库升级】master 数据源不是 MySQL，跳过迁移。url={}", url);
            return;
        }

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(master)
                    .locations(locations)
                    .encoding(encoding)
                    .sqlMigrationPrefix(sqlMigrationPrefix)
                    .sqlMigrationSeparator(sqlMigrationSeparator)
                    .placeholderPrefix(placeholderPrefix)
                    .placeholderSuffix(placeholderSuffix)
                    .sqlMigrationSuffixes(sqlMigrationSuffixes)
                    .validateOnMigrate(validateOnMigrate)
                    .baselineOnMigrate(baselineOnMigrate)
                    .cleanDisabled(cleanDisabled)
                    .load();
            int applied = flyway.migrate().migrationsExecuted;
            if (applied > 0) {
                log.info("【数据库升级】微服务 Flyway 执行了 {} 个迁移脚本。", applied);
            } else {
                log.info("【数据库升级】数据库结构已是最新，无需迁移。");
            }
        } catch (FlywayException e) {
            // 与单体版保持一致：迁移失败不阻止服务启动，但必须留下明显错误日志。
            log.error("【数据库升级】flyway执行sql脚本失败，请检查 flyway_schema_history 与脚本内容", e);
        }
    }
}
