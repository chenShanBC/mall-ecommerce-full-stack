package com.mallfei.pay.infrastructure.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import com.mallfei.pay.infrastructure.persistence.dataobject.PayOrderDO;
import com.mallfei.pay.infrastructure.persistence.mapper.PayOrderMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MybatisPayOrderRepository implements PayOrderRepository {

    private final PayOrderMapper payOrderMapper;

    public MybatisPayOrderRepository(PayOrderMapper payOrderMapper) {
        this.payOrderMapper = payOrderMapper;
    }

    @Override
    public PayOrder save(PayOrder payOrder) {
        PayOrderDO payOrderDO = toDO(payOrder);
        payOrderMapper.insert(payOrderDO);
        return toDomain(payOrderDO);
    }

    @Override
    public Optional<PayOrder> findByPayOrderNo(String payOrderNo) {
        PayOrderDO payOrderDO = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>().eq(PayOrderDO::getPayOrderNo, payOrderNo).last("limit 1"));
        return Optional.ofNullable(payOrderDO).map(this::toDomain);
    }

    @Override
    public Optional<PayOrder> findByOrderNo(String orderNo) {
        PayOrderDO payOrderDO = payOrderMapper.selectOne(new LambdaQueryWrapper<PayOrderDO>()
                .eq(PayOrderDO::getOrderNo, orderNo)
                .orderByDesc(PayOrderDO::getId)
                .last("limit 1"));
        return Optional.ofNullable(payOrderDO).map(this::toDomain);
    }

    @Override
    public List<PayOrder> findByOrderNoList(String orderNo) {
        return payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>()
                        .eq(PayOrderDO::getOrderNo, orderNo)
                        .orderByDesc(PayOrderDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayOrder> findPendingByOrderNo(String orderNo) {
        return payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>()
                        .eq(PayOrderDO::getOrderNo, orderNo)
                        .in(PayOrderDO::getPayStatus, List.of(PayOrder.STATUS_PENDING, PayOrder.STATUS_PAYING))
                        .orderByAsc(PayOrderDO::getId))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<PayOrder> findAll() {
        return payOrderMapper.selectList(new LambdaQueryWrapper<PayOrderDO>().orderByDesc(PayOrderDO::getId)).stream().map(this::toDomain).toList();
    }

    @Override
    public PageResult<PayOrder> search(String status, String keyword, long page, long size) {
        return search(status, keyword, page, size, null, null);
    }

    @Override
    public PageResult<PayOrder> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) {
        LambdaQueryWrapper<PayOrderDO> wrapper = new LambdaQueryWrapper<>();
        if (status != null && !status.isBlank()) wrapper.eq(PayOrderDO::getPayStatus, status);
        if (keyword != null && !keyword.isBlank()) wrapper.and(w -> w.like(PayOrderDO::getOrderNo, keyword.trim()).or().like(PayOrderDO::getPayOrderNo, keyword.trim()));
        applyPaySort(wrapper, sortBy, sortOrder);
        Page<PayOrderDO> result = payOrderMapper.selectPage(new Page<>(Math.max(page, 1), Math.max(size, 1)), wrapper);
        return new PageResult<>(result.getCurrent(), result.getSize(), result.getTotal(), result.getPages(), result.getRecords().stream().map(this::toDomain).toList());
    }

    @Override
    public long countAll() { return payOrderMapper.selectCount(null); }

    @Override
    public long countPending() {
        return payOrderMapper.selectCount(new LambdaQueryWrapper<PayOrderDO>().eq(PayOrderDO::getPayStatus, PayOrder.STATUS_PENDING));
    }

    @Override
    public long countSuccess() { return payOrderMapper.selectCount(new LambdaQueryWrapper<PayOrderDO>().eq(PayOrderDO::getPayStatus, PayOrder.STATUS_SUCCESS)); }

    @Override
    public long countClosed() { return payOrderMapper.selectCount(new LambdaQueryWrapper<PayOrderDO>().eq(PayOrderDO::getPayStatus, PayOrder.STATUS_CLOSED)); }

    @Override
    public void update(PayOrder payOrder) { payOrderMapper.updateById(toDO(payOrder)); }

    private void applyPaySort(LambdaQueryWrapper<PayOrderDO> wrapper, String sortBy, String sortOrder) {
        boolean asc = !"desc".equalsIgnoreCase(sortOrder);
        if (sortBy == null || sortBy.isBlank()) {
            wrapper.orderByAsc(PayOrderDO::getId);
            return;
        }
        switch (sortBy) {
            case "id" -> wrapper.orderBy(true, asc, PayOrderDO::getId);
            case "payOrderNo" -> wrapper.orderBy(true, asc, PayOrderDO::getPayOrderNo);
            case "orderNo" -> wrapper.orderBy(true, asc, PayOrderDO::getOrderNo);
            case "status" -> wrapper.orderBy(true, asc, PayOrderDO::getPayStatus);
            case "payAmount" -> wrapper.orderBy(true, asc, PayOrderDO::getPayAmountCent);
            default -> wrapper.orderByAsc(PayOrderDO::getId);
        }
    }

    private PayOrderDO toDO(PayOrder payOrder) {
        PayOrderDO payOrderDO = new PayOrderDO();
        payOrderDO.setId(payOrder.id());
        payOrderDO.setPayOrderNo(payOrder.payOrderNo());
        payOrderDO.setOrderNo(payOrder.orderNo());
        payOrderDO.setUserId(payOrder.userId());
        payOrderDO.setPayAmountCent(payOrder.payAmountCent());
        payOrderDO.setPayStatus(payOrder.payStatus());
        payOrderDO.setPayChannel(payOrder.payChannel());
        payOrderDO.setTransactionNo(payOrder.transactionNo());
        payOrderDO.setCallbackPayload(payOrder.callbackPayload());
        payOrderDO.setIdempotentKey(payOrder.idempotentKey());
        payOrderDO.setVersion(payOrder.version());
        return payOrderDO;
    }

    private PayOrder toDomain(PayOrderDO payOrderDO) {
        return new PayOrder(payOrderDO.getId(), payOrderDO.getPayOrderNo(), payOrderDO.getOrderNo(), payOrderDO.getUserId(), payOrderDO.getPayAmountCent(), payOrderDO.getPayStatus(), payOrderDO.getPayChannel(), payOrderDO.getTransactionNo(), payOrderDO.getIdempotentKey(), payOrderDO.getVersion(), payOrderDO.getCallbackPayload());
    }
}
