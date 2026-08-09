package com.example.ecapi.repository;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.CustomerOrderDetail;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 注文明細リポジトリ */
public interface CustomerOrderDetailRepository extends JpaRepository<CustomerOrderDetail, Long> {

    @Query(
            """
            SELECT d FROM CustomerOrderDetail d
            LEFT JOIN FETCH d.product
            WHERE d.order.id IN :orderIds
            """)
    List<CustomerOrderDetail> findAllByOrderIdIn(@Param("orderIds") List<Long> orderIds);

    // レビュー投稿可否の判定用: 指定顧客が指定商品を含む注文を指定ステータスで持っているか
    @Query(
            """
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END
            FROM CustomerOrderDetail d
            WHERE d.order.customer.id = :customerId
              AND d.product.id = :productId
              AND d.order.status = :status
              AND d.order.deleted = false
            """)
    boolean existsByCustomerIdAndProductIdAndOrderStatus(
            @Param("customerId") Long customerId,
            @Param("productId") Long productId,
            @Param("status") OrderStatus status);
}
