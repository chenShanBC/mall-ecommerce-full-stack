package com.mallfei.file.facade;

import com.mallfei.file.application.service.FileApplicationService;
import com.mallfei.file.application.vo.FileUploadResult;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileFacade {

    private final FileApplicationService fileApplicationService;

    public FileFacade(FileApplicationService fileApplicationService) {
        this.fileApplicationService = fileApplicationService;
    }

    public FileUploadResult uploadAvatar(MultipartFile file) {
        return fileApplicationService.uploadAvatar(file);
    }

    public FileUploadResult uploadProductImage(MultipartFile file) {
        return fileApplicationService.uploadProductImage(file);
    }
}
