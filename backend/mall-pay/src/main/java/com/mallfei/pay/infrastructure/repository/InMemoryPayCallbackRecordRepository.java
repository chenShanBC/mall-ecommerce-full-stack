package com.mallfei.pay.infrastructure.repository;

import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryPayCallbackRecordRepository implements PayCallbackRecordRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, PayCallbackRecord> storage = new ConcurrentHashMap<>();

    @Override
    public PayCallbackRecord save(PayCallbackRecord payCallbackRecord) {
        PayCallbackRecord persisted = new PayCallbackRecord(
                idGenerator.getAndIncrement(),
                payCallbackRecord.channel(),
                payCallbackRecord.payOrderNo(),
                payCallbackRecord.orderNo(),
                payCallbackRecord.outTradeNo(),
                payCallbackRecord.transactionNo(),
                payCallbackRecord.signature(),
                payCallbackRecord.verified(),
                payCallbackRecord.processStatus(),
                payCallbackRecord.rawPayload(),
                payCallbackRecord.callbackTime(),
                payCallbackRecord.createdAt(),
                payCallbackRecord.updatedAt()
        );
        storage.put(persisted.id(), persisted);
        return persisted;
    }

    @Override
    public Optional<PayCallbackRecord> findLatestByTransactionNo(String transactionNo) {
        return storage.values().stream()
                .filter(item -> transactionNo != null && transactionNo.equals(item.transactionNo()))
                .max(Comparator.comparing(PayCallbackRecord::id));
    }

    @Override
    public Optional<PayCallbackRecord> findLatestByOutTradeNo(String outTradeNo) {
        return storage.values().stream()
                .filter(item -> outTradeNo != null && outTradeNo.equals(item.outTradeNo()))
                .max(Comparator.comparing(PayCallbackRecord::id));
    }

    @Override
    public void update(PayCallbackRecord payCallbackRecord) {
        storage.put(payCallbackRecord.id(), payCallbackRecord);
    }
}
