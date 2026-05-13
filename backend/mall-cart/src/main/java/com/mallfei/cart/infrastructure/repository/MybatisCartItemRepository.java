package com.mallfei.cart.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.mallfei.cart.domain.model.CartItem;
import com.mallfei.cart.domain.repository.CartItemRepository;
import com.mallfei.cart.infrastructure.persistence.dataobject.CartItemDO;
import com.mallfei.cart.infrastructure.persistence.mapper.CartItemMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class MybatisCartItemRepository implements CartItemRepository {

    private final CartItemMapper cartItemMapper;

    public MybatisCartItemRepository(CartItemMapper cartItemMapper) {
        this.cartItemMapper = cartItemMapper;
    }

    @Override
    public List<CartItem> findByUserId(Long userId) {
        return cartItemMapper.selectList(new LambdaQueryWrapper<CartItemDO>()
                        .eq(CartItemDO::getUserId, userId)
                        .isNull(CartItemDO::getDeletedAt)
                        .orderByDesc(CartItemDO::getUpdatedAt)
                        .orderByDesc(CartItemDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<CartItem> findById(Long id) {
        CartItemDO item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemDO>()
                .eq(CartItemDO::getId, id)
                .isNull(CartItemDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public Optional<CartItem> findByUserIdAndSkuId(Long userId, Long skuId) {
        CartItemDO item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemDO>()
                .eq(CartItemDO::getUserId, userId)
                .eq(CartItemDO::getSkuId, skuId)
                .isNull(CartItemDO::getDeletedAt)
                .last("limit 1"));
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public Optional<CartItem> findDeletedByUserIdAndSkuId(Long userId, Long skuId) {
        CartItemDO item = cartItemMapper.selectOne(new LambdaQueryWrapper<CartItemDO>()
                .eq(CartItemDO::getUserId, userId)
                .eq(CartItemDO::getSkuId, skuId)
                .isNotNull(CartItemDO::getDeletedAt)
                .orderByDesc(CartItemDO::getUpdatedAt)
                .orderByDesc(CartItemDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(item).map(this::toDomain);
    }

    @Override
    public CartItem save(CartItem cartItem) {
        CartItemDO item = toDO(cartItem);
        LocalDateTime now = LocalDateTime.now();
        item.setCreatedAt(now);
        item.setUpdatedAt(now);
        cartItemMapper.insert(item);
        return toDomain(item);
    }

    @Override
    public CartItem update(CartItem cartItem) {
        LocalDateTime now = LocalDateTime.now();
        LambdaUpdateWrapper<CartItemDO> updateWrapper = new LambdaUpdateWrapper<CartItemDO>()
                .eq(CartItemDO::getId, cartItem.id())
                .set(CartItemDO::getUserId, cartItem.userId())
                .set(CartItemDO::getSkuId, cartItem.skuId())
                .set(CartItemDO::getQuantity, cartItem.quantity())
                .set(CartItemDO::getChecked, Boolean.TRUE.equals(cartItem.checked()) ? 1 : 0)
                .set(CartItemDO::getCreatedAt, cartItem.createdAt())
                .set(CartItemDO::getUpdatedAt, now)
                .set(CartItemDO::getDeletedAt, null);
        cartItemMapper.update(null, updateWrapper);
        return findById(cartItem.id()).orElseThrow();
    }

    @Override
    public void deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            CartItemDO update = new CartItemDO();
            update.setId(id);
            update.setDeletedAt(now);
            update.setUpdatedAt(now);
            cartItemMapper.updateById(update);
        }
    }

    @Override
    public void deleteByUserId(Long userId) {
        List<Long> ids = findByUserId(userId).stream().map(CartItem::id).toList();
        deleteByIds(ids);
    }

    private CartItem toDomain(CartItemDO item) {
        return new CartItem(
                item.getId(),
                item.getUserId(),
                item.getSkuId(),
                item.getQuantity(),
                item.getChecked() != null && item.getChecked() == 1,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private CartItemDO toDO(CartItem cartItem) {
        CartItemDO item = new CartItemDO();
        item.setId(cartItem.id());
        item.setUserId(cartItem.userId());
        item.setSkuId(cartItem.skuId());
        item.setQuantity(cartItem.quantity());
        item.setChecked(Boolean.TRUE.equals(cartItem.checked()) ? 1 : 0);
        item.setCreatedAt(cartItem.createdAt());
        item.setUpdatedAt(cartItem.updatedAt());
        item.setDeletedAt(null);
        return item;
    }
}
