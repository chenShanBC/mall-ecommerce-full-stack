package com.mallfei.file.infrastructure.storage;

import com.mallfei.common.exception.BusinessException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
@ConditionalOnProperty(prefix = "mall.file", name = "storage-type", havingValue = "minio")
public class MinioFileStorage implements FileStorage {

    @Override
    public StoredFile storeAvatar(MultipartFile file, String extension) {
        throw BusinessException.badRequest("当前环境暂未启用 MinIO 文件存储");
    }

    @Override
    public StoredFile storeProductImage(MultipartFile file, String extension) {
        throw BusinessException.badRequest("当前环境暂未启用 MinIO 文件存储");
    }
}
