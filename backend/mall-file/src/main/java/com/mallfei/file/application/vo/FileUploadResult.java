package com.mallfei.file.application.vo;

public record FileUploadResult(
        Long fileId,
        String storageType,
        String fileName,
        String originalFileName,
        String url,
        long size
) {
}
