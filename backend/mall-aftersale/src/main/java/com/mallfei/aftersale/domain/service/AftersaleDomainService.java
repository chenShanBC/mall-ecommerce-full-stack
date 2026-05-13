package com.mallfei.aftersale.domain.service;

import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.common.exception.BusinessException;
import org.springframework.stereotype.Service;

@Service
public class AftersaleDomainService {

    private final AftersaleOrderRepository aftersaleOrderRepository;

    public AftersaleDomainService(AftersaleOrderRepository aftersaleOrderRepository) {
        this.aftersaleOrderRepository = aftersaleOrderRepository;
    }

    public AftersaleOrder save(AftersaleOrder aftersaleOrder) {
        return aftersaleOrderRepository.save(aftersaleOrder);
    }

    public AftersaleOrder loadByAftersaleNo(String aftersaleNo) {
        return aftersaleOrderRepository.findByAftersaleNo(aftersaleNo).orElseThrow(() -> BusinessException.badRequest("售后单不存在"));
    }

    public void update(AftersaleOrder aftersaleOrder) {
        aftersaleOrderRepository.update(aftersaleOrder);
    }
}
