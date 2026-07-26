package com.example.ecapi.batch.reader;

import com.example.ecapi.batch.dto.PaymentSettlementProjection;
import com.example.ecapi.constant.PaymentStatus;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.StatelessSession;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamException;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * StatelessSession + キーセット方式でPAYMENT（status = CAPTURED）を読み取るReader（{@link
 * OrderDetailKeysetItemReader}と同じ設計方針）。
 *
 * <p>決済システム（自社の別システム、入金消込用）への出力元はPAYMENTテーブルそのものであり、
 * daily_sales_summary_by_product（商品単位に丸めた集計値でpayment_id・fee・net_amountを持たない）は経由しない。
 */
public class PaymentSettlementKeysetItemReader
        implements ItemStreamReader<PaymentSettlementProjection> {

    private static final String LAST_ID_KEY = "paymentSettlementReader.lastId";
    private static final int PAGE_SIZE = 500;

    private static final String QUERY =
            """
            SELECT new com.example.ecapi.batch.dto.PaymentSettlementProjection(
                p.id, p.customerOrder.id, p.transactionId, p.capturedAt, p.amount, p.fee, p.netAmount)
            FROM Payment p
            WHERE p.status = :status
              AND p.capturedAt BETWEEN :from AND :to
              AND p.id > :lastId
            ORDER BY p.id
            """;

    private final EntityManagerFactory entityManagerFactory;
    private final Instant from;
    private final Instant to;

    private StatelessSession session;
    private Deque<PaymentSettlementProjection> buffer = new ArrayDeque<>();
    private long lastId;

    public PaymentSettlementKeysetItemReader(
            EntityManagerFactory entityManagerFactory, Instant from, Instant to) {
        this.entityManagerFactory = entityManagerFactory;
        this.from = from;
        this.to = to;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.lastId = executionContext.getLong(LAST_ID_KEY, 0L);
        this.session = entityManagerFactory.unwrap(SessionFactory.class).openStatelessSession();
    }

    @Override
    public PaymentSettlementProjection read() {
        if (buffer.isEmpty()) {
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() {
        List<PaymentSettlementProjection> page =
                session.createQuery(QUERY, PaymentSettlementProjection.class)
                        .setParameter("status", PaymentStatus.CAPTURED)
                        .setParameter("from", from)
                        .setParameter("to", to)
                        .setParameter("lastId", lastId)
                        .setMaxResults(PAGE_SIZE)
                        .list();
        buffer.addAll(page);
        if (!page.isEmpty()) {
            lastId = page.get(page.size() - 1).id();
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        executionContext.putLong(LAST_ID_KEY, lastId);
    }

    @Override
    public void close() throws ItemStreamException {
        if (session != null) {
            session.close();
        }
    }
}
