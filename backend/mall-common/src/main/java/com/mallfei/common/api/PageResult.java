package com.mallfei.common.api;

import java.util.List;

public record PageResult<T>(
        long page,
        long size,
        long total,
        long pages,
        List<T> records
) {
    public static <T> PageResult<T> of(List<T> source, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.max(size, 1);
        long total = source.size();
        long pages = total == 0 ? 0 : (total + safeSize - 1) / safeSize;
        int fromIndex = (int) Math.min((safePage - 1) * safeSize, total);
        int toIndex = (int) Math.min(fromIndex + safeSize, total);
        return new PageResult<>(safePage, safeSize, total, pages, source.subList(fromIndex, toIndex));
    }
}
