package com.mallfei.file.controller;

import com.mallfei.common.api.ApiResponse;
import com.mallfei.common.auth.RequireLogin;
import com.mallfei.common.auth.RequireUser;
import com.mallfei.file.facade.FileFacade;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@Tag(name = "文件服务")
public class FileController {

    private final FileFacade fileFacade;

    public FileController(FileFacade fileFacade) {
        this.fileFacade = fileFacade;
    }

    @RequireUser
    @Operation(summary = "上传头像")
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadAvatar(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上传成功", fileFacade.uploadAvatar(file));
    }

    @RequireLogin
    @Operation(summary = "上传商品图片")
    @PostMapping(value = "/product-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<?> uploadProductImage(@RequestParam("file") MultipartFile file) {
        return ApiResponse.success("上传成功", fileFacade.uploadProductImage(file));
    }
}
