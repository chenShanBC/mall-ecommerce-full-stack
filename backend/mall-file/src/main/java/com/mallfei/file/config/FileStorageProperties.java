package com.mallfei.file.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mall.file")
public class FileStorageProperties {

    private String storageType = "local";
    private String uploadDir = "storage";
    private String publicBasePath = "/uploads";
    private long maxAvatarSize = 2097152L;

    public String getStorageType() {
        return storageType;
    }

    public void setStorageType(String storageType) {
        this.storageType = storageType;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getPublicBasePath() {
        return publicBasePath;
    }

    public void setPublicBasePath(String publicBasePath) {
        this.publicBasePath = publicBasePath;
    }

    public long getMaxAvatarSize() {
        return maxAvatarSize;
    }

    public void setMaxAvatarSize(long maxAvatarSize) {
        this.maxAvatarSize = maxAvatarSize;
    }
}
