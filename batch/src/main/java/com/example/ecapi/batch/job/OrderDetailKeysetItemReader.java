package com.example.ecapi.batch.job;

import com.example.ecapi.batch.dto.OrderDetailProjection;
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
 * StatelessSession + キーセット方式でCustomerOrderDetailを読み取るReader（14.7節①②参照）。
 * OFFSETページングは使わず、直前に読んだidより大きい行だけを都度取得することで PostgreSQLでのスキャンコスト増大（14.7節②）を回避する。
 */
public class OrderDetailKeysetItemReader implements ItemStreamReader<OrderDetailProjection> {

    private static final String LAST_ID_KEY = "orderDetailReader.lastId";
    private static final int PAGE_SIZE = 500;

    private static final String QUERY =
            """
            SELECT new com.example.ecapi.batch.dto.OrderDetailProjection(
                d.id, d.product.id, d.order.customer.id, d.unitPrice, d.quantity)
            FROM CustomerOrderDetail d
            WHERE d.order.id BETWEEN :minId AND :maxId
              AND d.createdAt BETWEEN :from AND :to
              AND d.id > :lastId
            ORDER BY d.id
            """;

    private final EntityManagerFactory entityManagerFactory;
    private final long minId;
    private final long maxId;
    private final Instant from;
    private final Instant to;

    private StatelessSession session;
    private Deque<OrderDetailProjection> buffer = new ArrayDeque<>();
    private long lastId;

    public OrderDetailKeysetItemReader(
            EntityManagerFactory entityManagerFactory,
            long minId,
            long maxId,
            Instant from,
            Instant to) {
        this.entityManagerFactory = entityManagerFactory;
        this.minId = minId;
        this.maxId = maxId;
        this.from = from;
        this.to = to;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        this.lastId = executionContext.getLong(LAST_ID_KEY, 0L);
        this.session = entityManagerFactory.unwrap(SessionFactory.class).openStatelessSession();
    }

    @Override
    public OrderDetailProjection read() {
        if (buffer.isEmpty()) {
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() {
        List<OrderDetailProjection> page =
                session.createQuery(QUERY, OrderDetailProjection.class)
                        .setParameter("minId", minId)
                        .setParameter("maxId", maxId)
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
