package com.mallfei.pay.infrastructure.repository;

import com.mallfei.common.api.PageResult;
import com.mallfei.pay.domain.model.PayCallbackRecord;
import com.mallfei.pay.domain.repository.PayCallbackRecordRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class InMemoryPayCallbackRecordRepository implements PayCallbackRecordRepository {

    private final AtomicLong idGenerator = new AtomicLong(1);
    private final Map<Long, PayCallbackRecord> storage = new ConcurrentHashMap<>();

    @Override
    public PayCallbackRecord save(PayCallbackRecord payCallbackRecord) {
        PayCallbackRecord persisted = new PayCallbackRecord(
                idGenerator.getAndIncrement(),
                payCallbackRecord.channel(),
                payCallbackRecord.callbackType(),
                payCallbackRecord.payOrderNo(),
                payCallbackRecord.refundNo(),
                payCallbackRecord.orderNo(),
                payCallbackRecord.outTradeNo(),
                payCallbackRecord.transactionNo(),
                payCallbackRecord.amountCent(),
                payCallbackRecord.tradeStatus(),
                payCallbackRecord.signature(),
                payCallbackRecord.verified(),
                payCallbackRecord.processStatus(),
                payCallbackRecord.failReason(),
                payCallbackRecord.rawPayload(),
                payCallbackRecord.callbackTime(),
                payCallbackRecord.processedAt(),
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
    public PageResult<PayCallbackRecord> search(String processStatus, String keyword, long page, long size) {
        long actualPage = Math.max(1, page);
        long actualSize = Math.max(1, size);
        List<PayCallbackRecord> rows = storage.values().stream()
                .filter(item -> processStatus == null || processStatus.isBlank() || processStatus.equalsIgnoreCase(item.processStatus()))
                .filter(item -> keyword == null || keyword.isBlank()
                        || contains(item.orderNo(), keyword)
                        || contains(item.payOrderNo(), keyword)
                        || contains(item.refundNo(), keyword)
                        || contains(item.transactionNo(), keyword)
                        || contains(item.outTradeNo(), keyword))
                .sorted(Comparator.comparing(PayCallbackRecord::id).reversed())
                .toList();
        long total = rows.size();
        long from = Math.min((actualPage - 1) * actualSize, total);
        long to = Math.min(from + actualSize, total);
        long pages = (total + actualSize - 1) / actualSize;
        return new PageResult<>(actualPage, actualSize, total, pages, rows.subList((int) from, (int) to));
    }

    @Override
    public void update(PayCallbackRecord payCallbackRecord) {
        storage.put(payCallbackRecord.id(), payCallbackRecord);
    }

    private boolean contains(String source, String keyword) {
        return source != null && keyword != null && source.contains(keyword.trim());
    }
}
