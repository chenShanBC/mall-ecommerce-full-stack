package com.mallfei.file.application.service;

import com.mallfei.auth.facade.AuthFacade;
import com.mallfei.common.auth.AuthenticatedPrincipal;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.file.application.vo.FileUploadResult;
import com.mallfei.file.config.FileStorageProperties;
import com.mallfei.file.domain.model.FileRecord;
import com.mallfei.file.domain.repository.FileRecordRepository;
import com.mallfei.file.domain.service.FileDomainService;
import com.mallfei.file.infrastructure.storage.FileStorage;
import com.mallfei.file.infrastructure.storage.StoredFile;
import com.mallfei.user.application.service.UserApplicationService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.sql.SQLSyntaxErrorException;

@Service
public class FileApplicationService {

    private final FileDomainService fileDomainService;
    private final FileStorage fileStorage;
    private final FileRecordRepository fileRecordRepository;
    private final FileStorageProperties fileStorageProperties;
    private final AuthFacade authFacade;
    private final UserApplicationService userApplicationService;

    public FileApplicationService(FileDomainService fileDomainService,
                                  FileStorage fileStorage,
                                  FileRecordRepository fileRecordRepository,
                                  FileStorageProperties fileStorageProperties,
                                  AuthFacade authFacade,
                                  UserApplicationService userApplicationService) {
        this.fileDomainService = fileDomainService;
        this.fileStorage = fileStorage;
        this.fileRecordRepository = fileRecordRepository;
        this.fileStorageProperties = fileStorageProperties;
        this.authFacade = authFacade;
        this.userApplicationService = userApplicationService;
    }

    public FileUploadResult uploadAvatar(MultipartFile file) {
        String extension = fileDomainService.validateAvatarFile(file);
        StoredFile storedFile = fileStorage.storeAvatar(file, extension);
        FileUploadResult result = saveFileRecord(storedFile, "USER_AVATAR", "头像上传失败");
        userApplicationService.updateCurrentUserAvatar(result.url());
        return result;
    }

    public FileUploadResult uploadProductImage(MultipartFile file) {
        String extension = fileDomainService.validateProductImageFile(file);
        StoredFile storedFile = fileStorage.storeProductImage(file, extension);
        return saveFileRecord(storedFile, "PRODUCT_IMAGE", "商品图片上传失败");
    }

    private FileUploadResult saveFileRecord(StoredFile storedFile, String bizType, String errorMessage) {
        String accessUrl = normalizePublicBasePath(fileStorageProperties.getPublicBasePath()) + "/" + storedFile.relativePath();
        AuthenticatedPrincipal principal = authFacade.currentRequiredPrincipal();
        try {
            FileRecord savedRecord = fileRecordRepository.save(new FileRecord(
                    null,
                    bizType,
                    storedFile.storageType(),
                    storedFile.fileName(),
                    storedFile.originalFileName(),
                    storedFile.relativePath(),
                    accessUrl,
                    storedFile.contentType(),
                    storedFile.size(),
                    principal.principalId(),
                    principal.identityType().name(),
                    principal.account(),
                    principal.nickname(),
                    "ACTIVE"
            ));
            return new FileUploadResult(
                    savedRecord.id(),
                    savedRecord.storageType(),
                    savedRecord.fileName(),
                    savedRecord.originalFileName(),
                    savedRecord.accessUrl(),
                    savedRecord.fileSize()
            );
        } catch (Exception ex) {
            if (isMissingFileRecordTable(ex)) {
                return new FileUploadResult(
                        null,
                        storedFile.storageType(),
                        storedFile.fileName(),
                        storedFile.originalFileName(),
                        accessUrl,
                        storedFile.size()
                );
            }
            throw BusinessException.badRequest(errorMessage);
        }
    }

    private boolean isMissingFileRecordTable(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLSyntaxErrorException syntaxErrorException
                    && syntaxErrorException.getMessage() != null
                    && syntaxErrorException.getMessage().contains("fms_file_record")) {
                return true;
            }
            current = current.getCause();
        }
        return false;
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
