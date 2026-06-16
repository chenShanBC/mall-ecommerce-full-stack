package com.mallfei.admin.application.service;

import com.mallfei.admin.application.dto.AdminProductSalesThresholdConfigRequest;
import com.mallfei.admin.application.vo.AdminProductSalesThresholdConfigView;
import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminOperationConfigApplicationService {

    private static final String HOT_SALES_THRESHOLD_KEY = "PRODUCT_HOT_SALES_THRESHOLD";
    private static final String LOW_SALES_THRESHOLD_KEY = "PRODUCT_LOW_SALES_THRESHOLD";
    private static final int DEFAULT_HOT_SALES_THRESHOLD = 100;
    private static final int DEFAULT_LOW_SALES_THRESHOLD = 10;

    private final JdbcTemplate jdbcTemplate;
    private final AuthFacade authFacade;
    private volatile boolean tableInitialized;

    public AdminOperationConfigApplicationService(JdbcTemplate jdbcTemplate, AuthFacade authFacade) {
        this.jdbcTemplate = jdbcTemplate;
        this.authFacade = authFacade;
    }

    public AdminProductSalesThresholdConfigView productSalesThresholdConfig() {
        ensureTable();
        Map<String, String> values = jdbcTemplate.queryForList("""
                        SELECT config_key, config_value
                        FROM admin_operation_config
                        WHERE config_key IN (?, ?)
                        """, HOT_SALES_THRESHOLD_KEY, LOW_SALES_THRESHOLD_KEY).stream()
                .collect(Collectors.toMap(row -> String.valueOf(row.get("config_key")), row -> String.valueOf(row.get("config_value")), (left, right) -> left));
        return new AdminProductSalesThresholdConfigView(
                parseInt(values.get(HOT_SALES_THRESHOLD_KEY), DEFAULT_HOT_SALES_THRESHOLD),
                parseInt(values.get(LOW_SALES_THRESHOLD_KEY), DEFAULT_LOW_SALES_THRESHOLD)
        );
    }

    public AdminProductSalesThresholdConfigView updateProductSalesThresholdConfig(AdminProductSalesThresholdConfigRequest request) {
        requireAdminManager();
        ensureTable();
        upsert(HOT_SALES_THRESHOLD_KEY, String.valueOf(request.hotSalesThreshold()), "商品近30天热销阈值");
        upsert(LOW_SALES_THRESHOLD_KEY, String.valueOf(request.lowSalesThreshold()), "商品近30天低销阈值");
        return productSalesThresholdConfig();
    }

    private void requireAdminManager() {
        AuthenticatedPrincipal principal = authFacade.currentPrincipal();
        if (principal == null || !principal.isAdmin() || !List.of("ADMIN", "SUPER_ADMIN").contains(principal.roleCode())) {
            throw BusinessException.forbidden("仅管理员可配置默认阈值");
        }
    }

    private void upsert(String key, String value, String description) {
        jdbcTemplate.update("""
                        INSERT INTO admin_operation_config (config_key, config_value, config_group, description, updated_at)
                        VALUES (?, ?, 'PRODUCT_SALES', ?, NOW())
                        ON DUPLICATE KEY UPDATE config_value = VALUES(config_value), description = VALUES(description), updated_at = NOW()
                        """, key, value, description);
    }

    private int parseInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 ? parsed : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void ensureTable() {
        if (tableInitialized) {
            return;
        }
        synchronized (this) {
            if (tableInitialized) {
                return;
            }
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS admin_operation_config (
                        id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                        config_key VARCHAR(128) NOT NULL COMMENT '配置键',
                        config_value VARCHAR(512) NOT NULL COMMENT '配置值',
                        config_group VARCHAR(64) DEFAULT NULL COMMENT '配置分组',
                        description VARCHAR(255) DEFAULT NULL COMMENT '配置说明',
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (id),
                        UNIQUE KEY uk_config_key (config_key)
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台运营轻量配置表'
                    """);
            tableInitialized = true;
        }
    }
}
