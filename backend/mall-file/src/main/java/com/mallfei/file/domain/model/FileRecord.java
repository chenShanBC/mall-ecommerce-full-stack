package com.mallfei.file.domain.model;

public record FileRecord(
        Long id,
        String bizType,
        String storageType,
        String fileName,
        String originalFileName,
        String relativePath,
        String accessUrl,
        String contentType,
        long fileSize,
        Long uploaderId,
        String uploaderType,
        String uploaderAccount,
        String uploaderNickname,
        String status
) {
}
