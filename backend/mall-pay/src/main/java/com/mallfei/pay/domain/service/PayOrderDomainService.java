package com.mallfei.pay.domain.service;

import com.mallfei.common.api.PageResult;
import com.mallfei.common.exception.BusinessException;
import com.mallfei.order.domain.model.Order;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PayOrderDomainService {

    private final PayOrderRepository payOrderRepository;

    public PayOrderDomainService(PayOrderRepository payOrderRepository) {
        this.payOrderRepository = payOrderRepository;
    }

    public PayOrder createPending(String payOrderNo, Order order, String payChannel) {
        ensureOrderCanCreatePayOrder(order, "当前订单状态不允许创建支付单");
        return PayOrder.createPending(payOrderNo, order.orderNo(), order.userId(), order.payAmountCent(), payChannel);
    }

    public PayOrder loadByPayOrderNo(String payOrderNo) { return payOrderRepository.findByPayOrderNo(payOrderNo).orElseThrow(() -> BusinessException.badRequest("支付单不存在")); }
    public PayOrder loadByOrderNo(String orderNo) { return payOrderRepository.findByOrderNo(orderNo).orElseThrow(() -> BusinessException.badRequest("支付单不存在")); }
    public PayOrder save(PayOrder payOrder) { return payOrderRepository.save(payOrder); }
    public void update(PayOrder payOrder) { payOrderRepository.update(payOrder); }
    public List<PayOrder> loadPendingByOrderNo(String orderNo) { return payOrderRepository.findPendingByOrderNo(orderNo); }
    public PageResult<PayOrder> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return payOrderRepository.search(status, keyword, page, size, sortBy, sortOrder); }
    public long countAll() { return payOrderRepository.countAll(); }
    public long countPending() { return payOrderRepository.countPending(); }
    public long countSuccess() { return payOrderRepository.countSuccess(); }
    public long countClosed() { return payOrderRepository.countClosed(); }
    public void ensureExistingPayOrderUsable(PayOrder payOrder, Order order) { if (!payOrder.reusableFor(order)) throw BusinessException.badRequest("当前订单状态不允许继续创建支付单"); }
    public void ensureCanCallbackSuccess(PayOrder payOrder, Order order) { if (!payOrder.canCallbackSuccessFor(order)) throw BusinessException.badRequest("当前支付单或订单状态不允许支付成功回调"); }
    public boolean reconcileAmount(Order order, PayOrder payOrder) { return payOrder == null || payOrder.amountConsistentWith(order.payAmountCent()); }
    public boolean reconcileStatus(Order order, PayOrder payOrder) { if (payOrder == null) return true; if (order.paymentException()) return false; if (payOrder.success() && !order.paidOrAfter()) return false; return !(payOrder.pending() && order.cancelled()); }

    private void ensureOrderCanCreatePayOrder(Order order, String message) { if (!order.pendingPayment()) throw BusinessException.badRequest(message); }
}
