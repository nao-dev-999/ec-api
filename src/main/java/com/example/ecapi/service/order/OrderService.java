package com.example.ecapi.service.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.CustomerOrderDetail;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.InsufficientStockException;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.CreateOrderItem;
import com.example.ecapi.service.order.dto.OrderResult;
import com.example.ecapi.service.order.dto.OrderResultItem;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;

    public List<OrderResult> findAll() {
        return orderRepository.findAll().stream().map(this::toOrderResult).toList();
    }

    /**
     * JOIN FETCH を使用して N+1 問題を回避します。
     *
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     */
    public OrderResult findById(Long id) {
        return orderRepository
                .findByIdWithItems(id)
                .map(this::toOrderResult)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * 在庫チェック → 在庫減算 → 注文作成を1トランザクションで実行します。
     *
     * @throws ProductNotFoundException 注文に含まれる商品が見つからない場合
     * @throws InsufficientStockException 在庫が不足している場合
     */
    @Transactional
    public OrderResult create(CreateOrder createOrder) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(createOrder.customerName());
        order.setStatus(OrderStatus.PENDING);
        order.setOrderedAt(Instant.now());

        for (CreateOrderItem item : createOrder.items()) {
            Product product =
                    productRepository
                            .findById(item.productId())
                            .orElseThrow(() -> new ProductNotFoundException(item.productId()));

            if (product.getStock() < item.quantity()) {
                throw new InsufficientStockException(
                        product.getName(), product.getStock(), item.quantity());
            }
            product.setStock(product.getStock() - item.quantity());

            CustomerOrderDetail detail = new CustomerOrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.quantity());
            detail.setUnitPrice(product.getPrice());
            detail.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
            order.getItems().add(detail);
        }

        order.setTotalAmount(
                order.getItems().stream()
                        .map(CustomerOrderDetail::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        return toOrderResult(orderRepository.save(order));
    }

    /**
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合
     */
    @Transactional
    public OrderResult updateStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order =
                orderRepository
                        .findById(id)
                        .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(newStatus);
        return toOrderResult(orderRepository.save(order));
    }

    /**
     * JOIN FETCH 済みの Product を直接更新します（N+1 回避、JPA dirty checking で保存されます）。
     *
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     */
    @Transactional
    public OrderResult cancel(Long id) {
        CustomerOrder order =
                orderRepository
                        .findByIdWithItems(id)
                        .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(OrderStatus.CANCELLED);
        order.getItems()
                .forEach(e -> e.getProduct().setStock(e.getProduct().getStock() + e.getQuantity()));
        return toOrderResult(orderRepository.save(order));
    }

    private OrderResult toOrderResult(CustomerOrder customerOrder) {
        return new OrderResult(
                customerOrder.getId(),
                customerOrder.getCustomerName(),
                customerOrder.getStatus(),
                customerOrder.getTotalAmount(),
                customerOrder.getItems().stream()
                        .map(
                                item ->
                                        new OrderResultItem(
                                                item.getProduct().getId(),
                                                item.getProduct().getName(),
                                                item.getQuantity(),
                                                item.getUnitPrice(),
                                                item.getSubtotal()))
                        .toList(),
                LocalDateTime.ofInstant(customerOrder.getOrderedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(customerOrder.getUpdatedAt(), ZoneId.systemDefault()),
                customerOrder.getVersion());
    }
}
