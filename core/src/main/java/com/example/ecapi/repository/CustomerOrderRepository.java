package com.example.ecapi.repository;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.CustomerOrder;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 注文リポジトリ */
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    /**
     * 明細(items)はコレクションのため JOIN FETCH と Pageable を併用すると Hibernate がメモリ上でページングしてしまう。
     * それを避けるため、この一覧取得では customer のみ JOIN FETCH し、明細は別クエリで取得する。
     */
    @Override
    @Query(
            value = "SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.customer",
            countQuery = "SELECT COUNT(o) FROM CustomerOrder o")
    Page<CustomerOrder> findAll(Pageable pageable);

    List<CustomerOrder> findByStatus(OrderStatus status);

    @Query("SELECT COALESCE(MAX(o.id), 0) FROM CustomerOrder o")
    Long findMaxId();

    @Query(
            """
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.customer
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id = :id
        """)
    Optional<CustomerOrder> findByIdWithItems(@Param("id") Long id);

    @Query(
            value =
                    "SELECT o FROM CustomerOrder o LEFT JOIN FETCH o.customer WHERE o.customer.id = :customerId",
            countQuery = "SELECT COUNT(o) FROM CustomerOrder o WHERE o.customer.id = :customerId")
    Page<CustomerOrder> findAllByCustomerId(
            @Param("customerId") Long customerId, Pageable pageable);

    @Query(
            """
        SELECT DISTINCT o FROM CustomerOrder o
        LEFT JOIN FETCH o.customer
        LEFT JOIN FETCH o.items i
        LEFT JOIN FETCH i.product
        WHERE o.id = :id AND o.customer.id = :customerId
        """)
    Optional<CustomerOrder> findByIdAndCustomerIdWithItems(
            @Param("id") Long id, @Param("customerId") Long customerId);
}
