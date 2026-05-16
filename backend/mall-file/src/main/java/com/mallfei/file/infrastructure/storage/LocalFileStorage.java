package com.mallfei.file.infrastructure.storage;

import com.mallfei.common.exception.BusinessException;
import com.mallfei.file.config.FileStorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class LocalFileStorage implements FileStorage {

    private static final DateTimeFormatter DATE_DIR_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private final FileStorageProperties fileStorageProperties;

    public LocalFileStorage(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public StoredFile storeAvatar(MultipartFile file, String extension) {
        String dateDir = DATE_DIR_FORMATTER.format(LocalDate.now());
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        String relativePath = "avatar/" + dateDir + "/" + fileName;

        Path rootDir = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        Path target = rootDir.resolve(relativePath).normalize();

        if (!target.startsWith(rootDir)) {
            throw BusinessException.badRequest("文件路径非法");
        }

        try {
            Files.createDirectories(target.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
            return new StoredFile(
                    normalizeStorageType(fileStorageProperties.getStorageType()),
                    fileName,
                    file.getOriginalFilename(),
                    relativePath.replace('\\', '/'),
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException ex) {
            throw BusinessException.badRequest("文件保存失败");
        }
    }

    private String normalizeStorageType(String storageType) {
        if (storageType == null || storageType.isBlank()) {
            return "local";
        }
        return storageType;
    }
}
