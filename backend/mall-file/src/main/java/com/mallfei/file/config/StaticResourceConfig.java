package com.mallfei.file.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@EnableConfigurationProperties(FileStorageProperties.class)
public class StaticResourceConfig implements WebMvcConfigurer {

    private final FileStorageProperties fileStorageProperties;

    public StaticResourceConfig(FileStorageProperties fileStorageProperties) {
        this.fileStorageProperties = fileStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicBasePath = normalizePublicBasePath(fileStorageProperties.getPublicBasePath());
        Path uploadPath = Paths.get(fileStorageProperties.getUploadDir()).toAbsolutePath().normalize();
        String resourceLocation = uploadPath.toUri().toString();

        registry.addResourceHandler(publicBasePath + "/**")
                .addResourceLocations(resourceLocation);

        // 兼容历史路径：/upload/** -> /uploads/**
        if (!"/upload".equals(publicBasePath)) {
            registry.addResourceHandler("/upload/**")
                    .addResourceLocations(resourceLocation);
        }
    }

    private String normalizePublicBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "/uploads";
        }
        String normalized = basePath.startsWith("/") ? basePath : "/" + basePath;
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
