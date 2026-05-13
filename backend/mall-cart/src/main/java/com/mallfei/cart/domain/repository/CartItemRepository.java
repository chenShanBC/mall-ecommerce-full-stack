package com.mallfei.cart.domain.repository;

import com.mallfei.cart.domain.model.CartItem;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository {

    List<CartItem> findByUserId(Long userId);

    Optional<CartItem> findById(Long id);

    Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId);

    Optional<CartItem> findDeletedByUserIdAndSkuId(Long userId, Long skuId);

    CartItem save(CartItem cartItem);

    CartItem update(CartItem cartItem);

    void deleteByIds(List<Long> ids);

    void deleteByUserId(Long userId);
}
