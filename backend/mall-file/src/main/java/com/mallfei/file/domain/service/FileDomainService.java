package com.mallfei.file.domain.service;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.file.config.FileStorageProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@Service
public class FileDomainService {

    private static final Set<String> ALLOWED_SUFFIXES = Set.of("jpg", "jpeg", "png", "webp");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final FileStorageProperties fileStorageProperties;

    public FileDomainService(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    public String validateAvatarFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw BusinessException.badRequest("请选择要上传的头像图片");
        }
        if (file.getSize() > fileStorageProperties.getMaxAvatarSize()) {
            throw BusinessException.badRequest("头像图片不能超过2MB");
        }
        String extension = resolveExtension(file.getOriginalFilename());
        if (!ALLOWED_SUFFIXES.contains(extension)) {
            throw BusinessException.badRequest("头像仅支持 jpg、jpeg、png、webp 格式");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw BusinessException.badRequest("头像文件类型不支持");
        }
        return extension;
    }

    public String resolveExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename) || !originalFilename.contains(".")) {
            throw BusinessException.badRequest("文件名不合法");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        if (!StringUtils.hasText(extension)) {
            throw BusinessException.badRequest("文件扩展名不合法");
        }
        return extension;
    }
}
