package com.mallfei.user.domain.model;

public record UserAddress(
        Long id,
        Long userId,
        String receiverName,
        String receiverPhone,
        String provinceCode,
        String provinceName,
        String cityCode,
        String cityName,
        String districtCode,
        String districtName,
        String detailAddress,
        String postalCode,
        boolean isDefault
) {

    public boolean belongsTo(Long targetUserId) {
        return userId != null && userId.equals(targetUserId);
    }

    public UserAddress markDefault() {
        if (isDefault) {
            return this;
        }
        return copy(true);
    }

    public UserAddress apply(String receiverName,
                             String receiverPhone,
                             String provinceCode,
                             String provinceName,
                             String cityCode,
                             String cityName,
                             String districtCode,
                             String districtName,
                             String detailAddress,
                             String postalCode,
                             boolean defaultAddress) {
        return new UserAddress(
                id,
                userId,
                receiverName,
                receiverPhone,
                emptyIfNull(provinceCode),
                provinceName,
                emptyIfNull(cityCode),
                cityName,
                emptyIfNull(districtCode),
                districtName,
                detailAddress,
                emptyIfNull(postalCode),
                defaultAddress
        );
    }

    private UserAddress copy(boolean defaultAddress) {
        return new UserAddress(
                id,
                userId,
                receiverName,
                receiverPhone,
                provinceCode,
                provinceName,
                cityCode,
                cityName,
                districtCode,
                districtName,
                detailAddress,
                postalCode,
                defaultAddress
        );
    }

    private static String emptyIfNull(String value) {
        return value == null ? "" : value;
    }
}
