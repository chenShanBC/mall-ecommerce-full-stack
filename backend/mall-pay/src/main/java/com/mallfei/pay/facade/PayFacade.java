package com.mallfei.pay.facade;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.application.service.PayApplicationService;
import com.mallfei.pay.application.vo.PayOrderPaymentVerificationView;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.model.PayReconciliationRecord;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import com.mallfei.pay.domain.repository.PayReconciliationRecordRepository;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PayFacade {

    private final PayOrderRepository payOrderRepository;
    private final PayCallbackRecordRepository payCallbackRecordRepository;
    private final PayReconciliationRecordRepository payReconciliationRecordRepository;
    private final PayApplicationService payApplicationService;

    public PayFacade(PayOrderRepository payOrderRepository,
                     PayCallbackRecordRepository payCallbackRecordRepository,
                     PayReconciliationRecordRepository payReconciliationRecordRepository,
                     PayApplicationService payApplicationService) {
        this.payOrderRepository = payOrderRepository;
        this.payCallbackRecordRepository = payCallbackRecordRepository;
        this.payReconciliationRecordRepository = payReconciliationRecordRepository;
        this.payApplicationService = payApplicationService;
    }

    public List<PayOrder> findAll() { return payOrderRepository.findAll(); }
    public PageResult<PayOrder> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return payOrderRepository.search(status, keyword, page, size, sortBy, sortOrder); }
    public long countAll() { return payOrderRepository.countAll(); }
    public long countPending() { return payOrderRepository.countPending(); }
    public long countSuccess() { return payOrderRepository.countSuccess(); }
    public long countClosed() { return payOrderRepository.countClosed(); }
    public Optional<PayOrder> findByOrderNo(String orderNo) { return payOrderRepository.findByOrderNo(orderNo); }
    public PageResult<PayCallbackRecord> searchCallbacks(String processStatus, String keyword, long page, long size) { return payCallbackRecordRepository.search(processStatus, keyword, page, size); }
    public PageResult<PayReconciliationRecord> searchReconciliationRecords(String bizType, Boolean consistent, String repairStatus, String keyword, long page, long size) { return payReconciliationRecordRepository.search(bizType, consistent, repairStatus, keyword, page, size); }
    public long countReconciliationRecords(String bizType, Boolean consistent, String repairStatus) { return payReconciliationRecordRepository.count(bizType, consistent, repairStatus); }
    public PayReconciliationRecord getReconciliationRecord(Long id) { return payReconciliationRecordRepository.findById(id).orElseThrow(() -> com.mallfei.common.exception.BusinessException.badRequest("对账记录不存在")); }
    public PayReconciliationRecord markReconciliationRecordIgnored(Long id, String remark) { PayReconciliationRecord record = getReconciliationRecord(id).markIgnored(remark); payReconciliationRecordRepository.update(record); return record; }
    public PayReconciliationRecord markReconciliationRecordDone(Long id, String remark) { PayReconciliationRecord record = getReconciliationRecord(id).markRepairDone(remark); payReconciliationRecordRepository.update(record); return record; }
    public PayReconciliationRecord markReconciliationRecordStatus(Long id, String repairStatus, String remark) { PayReconciliationRecord record = getReconciliationRecord(id).markHandlingStatus(repairStatus, remark); payReconciliationRecordRepository.update(record); return record; }
    public PayOrderView closePayOrder(String orderNo, String reason) { return payApplicationService.closePayOrder(orderNo, reason); }
    public PayOrderView handleChannelCallback(PayChannelCallbackRequest request) { return payApplicationService.handleChannelCallback(request); }
    public PayOrderPaymentVerificationView verifyOrderPayment(String orderNo) { return payApplicationService.verifyOrderPayment(orderNo); }
    public PayReconcileResultView reconcile(String orderNo) { return payApplicationService.reconcile(orderNo); }
    public PayOrderView refund(String orderNo, Long refundAmountCent, String reason, String refundNo) { return payApplicationService.refund(orderNo, refundAmountCent, reason, refundNo); }
    public PayOrderView reconcileRefund(String orderNo, String refundNo, Long refundAmountCent) { return payApplicationService.reconcileRefund(orderNo, refundNo, refundAmountCent); }
    public PayOrderView syncOrderStatus(String orderNo) { return payApplicationService.syncOrderStatus(orderNo); }
    public PayOrderView repairPaidOrder(String orderNo) { return payApplicationService.repairPaidOrder(orderNo); }
    public PayReconciliationRecord transferPaySync(String orderNo, String remark) { return payApplicationService.transferPaySync(orderNo, remark); }
    public PayReconciliationRecord markPaymentExceptionPendingAction(String orderNo, String action, String remark) { return payApplicationService.markPaymentExceptionPendingAction(orderNo, action, remark); }
}
