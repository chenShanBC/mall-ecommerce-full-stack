package com.mallfei.aftersale.facade;

import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import com.mallfei.aftersale.domain.service.AftersaleDomainService;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class AftersaleFacade {

    private final AftersaleDomainService aftersaleDomainService;
    private final AftersaleOrderRepository aftersaleOrderRepository;

    public AftersaleFacade(AftersaleDomainService aftersaleDomainService,
                           AftersaleOrderRepository aftersaleOrderRepository) {
        this.aftersaleDomainService = aftersaleDomainService;
        this.aftersaleOrderRepository = aftersaleOrderRepository;
    }

    public List<AftersaleOrder> findAll() {
        return aftersaleOrderRepository.findAll();
    }

    public AftersaleOrder getByAftersaleNo(String aftersaleNo) {
        return aftersaleDomainService.loadByAftersaleNo(aftersaleNo);
    }

    public AftersaleOrder approve(String aftersaleNo) {
        AftersaleOrder approved = getByAftersaleNo(aftersaleNo).approve(LocalDateTime.now());
        aftersaleDomainService.update(approved);
        return getByAftersaleNo(aftersaleNo);
    }

    public AftersaleOrder reject(String aftersaleNo) {
        AftersaleOrder rejected = getByAftersaleNo(aftersaleNo).reject(LocalDateTime.now());
        aftersaleDomainService.update(rejected);
        return getByAftersaleNo(aftersaleNo);
    }
}
