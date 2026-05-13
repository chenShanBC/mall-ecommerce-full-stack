package com.mallfei.cart.domain.model;

import java.time.LocalDateTime;

public record CartItem(
        Long id,
        Long userId,
        Long skuId,
        Integer quantity,
        Boolean checked,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public boolean checkedForCheckout() {
        return Boolean.TRUE.equals(checked);
    }

    public long subtotalAmount(long unitPriceCent) {
        return unitPriceCent * quantity;
    }
}
