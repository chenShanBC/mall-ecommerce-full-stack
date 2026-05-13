package com.mallfei.user.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "UserAddressCreateRequest", description = "新增用户地址请求")
public record UserAddressCreateRequest(
        @Schema(description = "收件人姓名", example = "Manual Receiver")
        @NotBlank(message = "收件人姓名不能为空") String receiverName,
        @Schema(description = "收件人手机号", example = "13800138000")
        @NotBlank(message = "收件人手机号不能为空") String receiverPhone,
        @Schema(description = "省编码", example = "110000")
        String provinceCode,
        @Schema(description = "省名称", example = "北京市")
        @NotBlank(message = "省名称不能为空") String provinceName,
        @Schema(description = "市编码", example = "110100")
        String cityCode,
        @Schema(description = "市名称", example = "北京市")
        @NotBlank(message = "市名称不能为空") String cityName,
        @Schema(description = "区编码", example = "110105")
        String districtCode,
        @Schema(description = "区名称", example = "朝阳区")
        @NotBlank(message = "区名称不能为空") String districtName,
        @Schema(description = "详细地址", example = "望京 SOHO T1 1001")
        @NotBlank(message = "详细地址不能为空") @Size(max = 255, message = "详细地址长度不能超过255") String detailAddress,
        @Schema(description = "邮政编码", example = "100000")
        String postalCode,
        @Schema(description = "是否默认地址", example = "true")
        boolean isDefault
) {
}
