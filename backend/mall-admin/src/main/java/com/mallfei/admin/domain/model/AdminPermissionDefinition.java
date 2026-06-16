package com.mallfei.admin.domain.model;

/**
 * 后台权限元数据。权限编码是系统鉴权契约，名称和分组用于管理端展示。
 */
public record AdminPermissionDefinition(
        String code,
        String name,
        String groupCode,
        String groupName,
        boolean sensitive
) {
}
