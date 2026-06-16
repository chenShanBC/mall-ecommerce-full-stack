package com.mallfei.admin.application.dto;



import jakarta.validation.constraints.NotBlank;



public record AdminOrderReceiverUpdateRequest(

        @NotBlank(message = "收货人不能为空") String receiverName,

        @NotBlank(message = "收货电话不能为空") String receiverPhone,

        @NotBlank(message = "收货省份不能为空") String receiverProvinceName,

        @NotBlank(message = "收货城市不能为空") String receiverCityName,

        @NotBlank(message = "收货区县不能为空") String receiverDistrictName,

        @NotBlank(message = "收货详细地址不能为空") String receiverDetailAddress,

        String note

) {

}

