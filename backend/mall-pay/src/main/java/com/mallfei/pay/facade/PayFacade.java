package com.mallfei.pay.facade;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.application.service.PayApplicationService;
import com.mallfei.pay.application.vo.PayOrderView;
import com.mallfei.pay.application.vo.PayReconcileResultView;
import com.mallfei.pay.domain.model.PayOrder;
import com.mallfei.pay.domain.repository.PayOrderRepository;
import com.mallfei.pay.domain.service.PayChannelCallbackRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class PayFacade {

    private final PayOrderRepository payOrderRepository;
    private final PayApplicationService payApplicationService;

    public PayFacade(PayOrderRepository payOrderRepository,
                     PayApplicationService payApplicationService) {
        this.payOrderRepository = payOrderRepository;
        this.payApplicationService = payApplicationService;
    }

    public List<PayOrder> findAll() { return payOrderRepository.findAll(); }
    public PageResult<PayOrder> search(String status, String keyword, long page, long size, String sortBy, String sortOrder) { return payOrderRepository.search(status, keyword, page, size, sortBy, sortOrder); }
    public long countAll() { return payOrderRepository.countAll(); }
    public long countPending() { return payOrderRepository.countPending(); }
    public long countSuccess() { return payOrderRepository.countSuccess(); }
    public long countClosed() { return payOrderRepository.countClosed(); }
    public Optional<PayOrder> findByOrderNo(String orderNo) { return payOrderRepository.findByOrderNo(orderNo); }
    public PayOrderView closePayOrder(String orderNo, String reason) { return payApplicationService.closePayOrder(orderNo, reason); }
    public PayOrderView handleChannelCallback(PayChannelCallbackRequest request) { return payApplicationService.handleChannelCallback(request); }
    public PayReconcileResultView reconcile(String orderNo) { return payApplicationService.reconcile(orderNo); }
    public PayOrderView syncOrderStatus(String orderNo) { return payApplicationService.syncOrderStatus(orderNo); }
    public PayOrderView repairPaidOrder(String orderNo) { return payApplicationService.repairPaidOrder(orderNo); }
}
