package com.mallfei.aftersale.infrastructure.repository;

import com.mallfei.aftersale.domain.model.AftersaleOrder;
import com.mallfei.aftersale.domain.repository.AftersaleOrderRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryAftersaleOrderRepository implements AftersaleOrderRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<String, AftersaleOrder> storage = new ConcurrentHashMap<>();

    @Override
    public AftersaleOrder save(AftersaleOrder aftersaleOrder) {
        AftersaleOrder persisted = new AftersaleOrder(
                idGenerator.getAndIncrement(),
                aftersaleOrder.aftersaleNo(),
                aftersaleOrder.orderNo(),
                aftersaleOrder.userId(),
                aftersaleOrder.aftersaleType(),
                aftersaleOrder.status(),
                aftersaleOrder.refundAmountCent(),
                aftersaleOrder.reason(),
                aftersaleOrder.version(),
                aftersaleOrder.createdAt(),
                aftersaleOrder.updatedAt()
        );
        storage.put(persisted.aftersaleNo(), persisted);
        return persisted;
    }

    @Override
    public Optional<AftersaleOrder> findByAftersaleNo(String aftersaleNo) {
        return Optional.ofNullable(storage.get(aftersaleNo));
    }

    @Override
    public Optional<AftersaleOrder> findLatestByOrderNo(String orderNo) {
        return storage.values().stream()
                .filter(item -> orderNo.equals(item.orderNo()))
                .max(Comparator.comparing(AftersaleOrder::createdAt));
    }

    @Override
    public List<AftersaleOrder> findAll() {
        return storage.values().stream()
                .sorted(Comparator.comparing(AftersaleOrder::createdAt).reversed())
                .toList();
    }

    @Override
    public void update(AftersaleOrder aftersaleOrder) {
        storage.put(aftersaleOrder.aftersaleNo(), aftersaleOrder);
    }
}
