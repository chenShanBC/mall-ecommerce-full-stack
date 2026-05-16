package com.mallfei.file.infrastructure.storage;

public record StoredFile(
        String storageType,
        String fileName,
        String originalFileName,
        String relativePath,
        String contentType,
        long size
) {
}
