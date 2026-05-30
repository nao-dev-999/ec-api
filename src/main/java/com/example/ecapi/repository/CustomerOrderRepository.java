package com.example.ecapi.repository;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 注文リポジトリ */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    @Override
    @Query(
            """
            SELECT
              DISTINCT o
            FROM CustomerOrder o
              LEFT JOIN FETCH o.items i
              LEFT JOIN FETCH i.product
          """)
    List<CustomerOrder> findAll();

    List<CustomerOrder> findByCustomerName(String customerName);

    List<CustomerOrder> findByStatus(OrderStatus status);

    @Query(
            """
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id = :id
        """)
    Optional<CustomerOrder> findByIdWithItems(@Param("id") Long id);
}
