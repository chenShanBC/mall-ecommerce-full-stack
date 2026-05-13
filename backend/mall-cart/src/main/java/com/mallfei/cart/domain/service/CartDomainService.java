package com.mallfei.cart.domain.service;

import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.cart.domain.repository.CartItemRepository;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CartDomainService {

    private final CartItemRepository cartItemRepository;

    public CartDomainService(CartItemRepository cartItemRepository) {
        this.cartItemRepository = cartItemRepository;
    }

    public List<CartItem> loadUserCartItems(Long userId) {
        return cartItemRepository.findByUserId(userId);
    }

    public CartItem loadOwnedItem(Long userId, Long cartItemId) {
        CartItem item = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> BusinessException.badRequest("购物车项不存在"));
        if (!item.userId().equals(userId)) {
            throw BusinessException.forbidden("无权操作当前购物车项");
        }
        return item;
    }

    public List<CartItem> loadOwnedItems(Long userId, List<Long> cartItemIds) {
        List<Long> uniqueIds = cartItemIds.stream().distinct().toList();
        List<CartItem> items = uniqueIds.stream().map(id -> loadOwnedItem(userId, id)).toList();
        if (items.isEmpty()) {
            throw BusinessException.badRequest("购物车项不存在");
        }
        return items;
    }

    public CartItem addItem(Long userId, Long skuId, Integer quantity, Boolean checked) {
        CartItem existing = cartItemRepository.findByUserIdAndSkuId(userId, skuId).orElse(null);
        if (existing != null) {
            return cartItemRepository.update(new CartItem(
                    existing.id(),
                    existing.userId(),
                    existing.skuId(),
                    existing.quantity() + quantity,
                    checked != null ? checked : existing.checked(),
                    existing.createdAt(),
                    LocalDateTime.now()
            ));
        }

        CartItem deleted = cartItemRepository.findDeletedByUserIdAndSkuId(userId, skuId).orElse(null);
        if (deleted != null) {
            return cartItemRepository.update(new CartItem(
                    deleted.id(),
                    deleted.userId(),
                    deleted.skuId(),
                    quantity,
                    checked == null || checked,
                    deleted.createdAt(),
                    LocalDateTime.now()
            ));
        }

        return cartItemRepository.save(new CartItem(
                null,
                userId,
                skuId,
                quantity,
                checked == null || checked,
                LocalDateTime.now(),
                LocalDateTime.now()
        ));
    }

    public CartItem updateItem(CartItem existing, Long skuId, Integer quantity, Boolean checked) {
        return cartItemRepository.update(new CartItem(
                existing.id(),
                existing.userId(),
                skuId == null ? existing.skuId() : skuId,
                quantity == null ? existing.quantity() : quantity,
                checked == null ? existing.checked() : checked,
                existing.createdAt(),
                LocalDateTime.now()
        ));
    }

    public void ensureCanCheckout(List<CartItem> checkedItems) {
        if (checkedItems.isEmpty()) {
            throw BusinessException.badRequest("未勾选可结算购物车项");
        }
    }

    public List<CartItem> loadCheckedItems(Long userId) {
        return loadUserCartItems(userId).stream()
                .filter(CartItem::checkedForCheckout)
                .toList();
    }

    public void removeItem(Long userId, Long cartItemId) {
        CartItem item = loadOwnedItem(userId, cartItemId);
        cartItemRepository.deleteByIds(List.of(item.id()));
    }

    public void removeItems(List<CartItem> items) {
        cartItemRepository.deleteByIds(items.stream().map(CartItem::id).toList());
    }

    public void removeCheckedItems(List<CartItem> checkedItems) {
        removeItems(checkedItems);
    }

    public void clear(Long userId) {
        cartItemRepository.deleteByUserId(userId);
    }

    public int totalQuantity(Long userId) {
        return loadUserCartItems(userId).stream().mapToInt(CartItem::quantity).sum();
    }
}
