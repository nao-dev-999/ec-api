package com.example.ecapi.repository;

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
}
