package com.mallfei.common.api;

import java.util.List;

public record PageResponse<T>(
        List<T> records,
        long total,
        long pageNum,
        long pageSize
) {
}
