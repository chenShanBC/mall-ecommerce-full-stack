package com.mallfei.product.domain.model;

public record Category(
        Long id,
        String name,
        Long parentId,
        Integer level,
        Integer sortOrder,
        String status
) {

    public boolean root() {
        return parentId != null && parentId == 0L;
    }

    public Category applyUpdate(String name, Long parentId, Integer sortOrder, String status) {
        Long normalizedParentId = parentId == null ? 0L : parentId;
        int normalizedLevel = normalizedParentId > 0 ? 2 : 1;
        return new Category(
                id,
                name,
                normalizedParentId,
                normalizedLevel,
                sortOrder == null ? this.sortOrder : sortOrder,
                normalizeStatus(status, this.status)
        );
    }

    public Category applyStatus(String status) {
        return new Category(id, name, parentId, level, sortOrder, normalizeStatus(status, this.status));
    }

    public static Category create(String name, Long parentId, Integer sortOrder) {
        Long normalizedParentId = parentId == null ? 0L : parentId;
        int normalizedLevel = normalizedParentId > 0 ? 2 : 1;
        return new Category(null, name, normalizedParentId, normalizedLevel, sortOrder == null ? 0 : sortOrder, "ENABLED");
    }

    private static String normalizeStatus(String status, String fallback) {
        if ("ENABLED".equalsIgnoreCase(status)) {
            return "ENABLED";
        }
        if ("DISABLED".equalsIgnoreCase(status)) {
            return "DISABLED";
        }
        return fallback == null ? "ENABLED" : fallback;
    }
}
