package com.mallfei.file.infrastructure.storage;

import org.springframework.web.multipart.MultipartFile;

public interface FileStorage {

    StoredFile storeAvatar(MultipartFile file, String extension);

    StoredFile storeProductImage(MultipartFile file, String extension);
}
