package com.example.ecapi.batch.job.salesaggregation;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.ecapi.batch.config.BatchAuditConfig;
import com.example.ecapi.batch.dto.OrderDetailProjection;
import com.example.ecapi.batch.support.TestcontainersConfiguration;
import com.example.ecapi.constant.OrderPaymentStatus;
import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.constant.PaymentStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.CustomerOrderDetail;
import com.example.ecapi.entity.Payment;
import com.example.ecapi.entity.Product;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.transaction.TestTransaction;

/**
 * PAYMENTとの結合が実DB（Testcontainers）上で正しく機能することを検証する。
 *
 * <p>OrderDetailKeysetItemReaderはStatelessSessionで別セッション（別トランザクション）として読むため、
 * TestEntityManagerで作成したフィクスチャは{@link TestTransaction#end()}でコミットしてから読む必要がある
 * （コミットしない限りReader側のセッションからは見えない）。コミット後はテスト終了時の自動ロールバックが効かないため、 {@link
 * #cleanUpCommittedFixtures()}で明示的にTRUNCATEする。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EntityScan("com.example.ecapi.entity")
@Import({TestcontainersConfiguration.class, BatchAuditConfig.class})
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class OrderDetailKeysetItemReaderTest {

    @Autowired private TestEntityManager entityManager;
    @Autowired private EntityManagerFactory entityManagerFactory;

    private Customer persistCustomer() {
        Customer customer = new Customer();
        customer.setEmail("reader-test-" + System.nanoTime() + "@example.com");
        customer.setPassword("hashed_password");
        return entityManager.persistFlushFind(customer);
    }

    private Product persistProduct() {
        Product product = new Product();
        product.setName("テスト商品");
        product.setPrice(BigDecimal.valueOf(1000));
        product.setStock(10);
        return entityManager.persistFlushFind(product);
    }

    private CustomerOrder persistOrder(Customer customer, OrderPaymentStatus paymentStatus) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPaymentStatus(paymentStatus);
        order.setOrderedAt(Instant.now());
        order.setTotalAmount(BigDecimal.valueOf(2000));
        return entityManager.persistFlushFind(order);
    }

    private CustomerOrderDetail persistOrderDetail(CustomerOrder order, Product product) {
        CustomerOrderDetail detail = new CustomerOrderDetail();
        detail.setProduct(product);
        detail.setQuantity(2);
        detail.setUnitPrice(product.getPrice());
        detail.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(2)));
        order.addItem(detail);
        return entityManager.persistFlushFind(detail);
    }

    private void persistPayment(CustomerOrder order, PaymentStatus status, Instant capturedAt) {
        Payment payment = new Payment();
        payment.setCustomerOrder(order);
        payment.setTransactionId("txn-" + order.getId());
        payment.setStatus(status);
        payment.setAmount(BigDecimal.valueOf(2000));
        payment.setFee(BigDecimal.valueOf(60));
        payment.setNetAmount(BigDecimal.valueOf(1940));
        payment.setAuthorizedAt(Instant.now());
        payment.setCapturedAt(capturedAt);
        entityManager.persistFlushFind(payment);
    }

    private void commitFixtures() {
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    @AfterEach
    void cleanUpCommittedFixtures() {
        TestTransaction.start();
        entityManager
                .getEntityManager()
                .createNativeQuery(
                        "TRUNCATE TABLE payment, customer_order_detail, customer_order, product,"
                                + " customer RESTART IDENTITY CASCADE")
                .executeUpdate();
        TestTransaction.flagForCommit();
        TestTransaction.end();
    }

    private List<OrderDetailProjection> readAll(Instant from, Instant to) throws Exception {
        OrderDetailKeysetItemReader reader =
                new OrderDetailKeysetItemReader(entityManagerFactory, 0L, Long.MAX_VALUE, from, to);
        reader.open(new ExecutionContext());
        try {
            List<OrderDetailProjection> results = new ArrayList<>();
            OrderDetailProjection item;
            while ((item = reader.read()) != null) {
                results.add(item);
            }
            return results;
        } finally {
            reader.close();
        }
    }

    @Test
    @DisplayName("PAYMENT.statusがCAPTURED以外（AUTHORIZED/FAILED/REFUNDED）の注文明細は集計対象から除外されること")
    void shouldExcludeOrderDetailsWhosePaymentIsNotCaptured() throws Exception {
        Instant from = Instant.parse("2026-07-25T15:00:00Z");
        Instant to = Instant.parse("2026-07-26T15:00:00Z");
        Instant capturedAt = Instant.parse("2026-07-26T03:00:00Z");
        Customer customer = persistCustomer();
        Product product = persistProduct();

        CustomerOrder capturedOrder = persistOrder(customer, OrderPaymentStatus.CAPTURED);
        CustomerOrderDetail capturedDetail = persistOrderDetail(capturedOrder, product);
        persistPayment(capturedOrder, PaymentStatus.CAPTURED, capturedAt);

        CustomerOrder authorizedOrder = persistOrder(customer, OrderPaymentStatus.AUTHORIZED);
        persistOrderDetail(authorizedOrder, product);
        persistPayment(authorizedOrder, PaymentStatus.AUTHORIZED, null);

        CustomerOrder cancelledOrder = persistOrder(customer, OrderPaymentStatus.CANCELLED);
        persistOrderDetail(cancelledOrder, product);
        persistPayment(cancelledOrder, PaymentStatus.FAILED, null);

        // 一度CAPTUREDになってから返金された想定。captured_atは元のCAPTURED時点のまま対象範囲内に残るが、
        // statusがREFUNDEDである以上は売上集計の対象外にならなければならない。
        CustomerOrder refundedOrder = persistOrder(customer, OrderPaymentStatus.REFUNDED);
        persistOrderDetail(refundedOrder, product);
        persistPayment(refundedOrder, PaymentStatus.REFUNDED, capturedAt);

        commitFixtures();

        List<OrderDetailProjection> result = readAll(from, to);

        assertThat(result)
                .extracting(OrderDetailProjection::id)
                .containsExactly(capturedDetail.getId());
    }

    @Test
    @DisplayName("captured_atが対象日時範囲外の注文明細は除外され、範囲内（境界値含む）のみ対象になること")
    void shouldFilterByCapturedAtDateRangeInclusiveOfBoundaries() throws Exception {
        Instant from = Instant.parse("2026-07-25T15:00:00Z");
        Instant to = Instant.parse("2026-07-26T15:00:00Z");
        Customer customer = persistCustomer();
        Product product = persistProduct();

        CustomerOrder beforeRangeOrder = persistOrder(customer, OrderPaymentStatus.CAPTURED);
        persistOrderDetail(beforeRangeOrder, product);
        persistPayment(beforeRangeOrder, PaymentStatus.CAPTURED, from.minusSeconds(1));

        CustomerOrder afterRangeOrder = persistOrder(customer, OrderPaymentStatus.CAPTURED);
        persistOrderDetail(afterRangeOrder, product);
        persistPayment(afterRangeOrder, PaymentStatus.CAPTURED, to.plusSeconds(1));

        CustomerOrder atFromBoundaryOrder = persistOrder(customer, OrderPaymentStatus.CAPTURED);
        CustomerOrderDetail atFromBoundaryDetail = persistOrderDetail(atFromBoundaryOrder, product);
        persistPayment(atFromBoundaryOrder, PaymentStatus.CAPTURED, from);

        CustomerOrder atToBoundaryOrder = persistOrder(customer, OrderPaymentStatus.CAPTURED);
        CustomerOrderDetail atToBoundaryDetail = persistOrderDetail(atToBoundaryOrder, product);
        persistPayment(atToBoundaryOrder, PaymentStatus.CAPTURED, to);

        commitFixtures();

        List<OrderDetailProjection> result = readAll(from, to);

        assertThat(result)
                .extracting(OrderDetailProjection::id)
                .containsExactlyInAnyOrder(
                        atFromBoundaryDetail.getId(), atToBoundaryDetail.getId());
    }
}
