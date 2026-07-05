package com.example.ecapi.service.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.entity.Customer;
import com.example.ecapi.entity.CustomerOrder;
import com.example.ecapi.entity.CustomerOrderDetail;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.CustomerNotFoundException;
import com.example.ecapi.exception.InsufficientStockException;
import com.example.ecapi.exception.OrderNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CustomerOrderDetailRepository;
import com.example.ecapi.repository.CustomerOrderRepository;
import com.example.ecapi.repository.CustomerRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.cart.CartService;
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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerOrderDetailRepository orderDetailRepository;
    private final ProductRepository productRepository;
    private final CustomerRepository customerRepository;
    private final CartService cartService;

    public Page<OrderResult> findAll(Pageable pageable) {
        return toOrderResultPage(orderRepository.findAll(pageable));
    }

    /** ログイン中の顧客自身の注文のみを、新しい順に取得します。 */
    public Page<OrderResult> findAllByCustomerId(Long customerId, Pageable pageable) {
        return toOrderResultPage(orderRepository.findAllByCustomerId(customerId, pageable));
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
     * ログイン中の顧客自身の注文かどうかを検証したうえで取得します。他顧客の注文の場合は存在しないものとして扱います。
     *
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない、または他顧客の注文の場合
     */
    public OrderResult findByIdForCustomer(Long id, Long customerId) {
        return orderRepository
                .findByIdAndCustomerIdWithItems(id, customerId)
                .map(this::toOrderResult)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    /**
     * 在庫チェック → 在庫減算 → 注文作成を1トランザクションで実行します。
     *
     * @throws CustomerNotFoundException 顧客が見つからない場合
     * @throws ProductNotFoundException 注文に含まれる商品が見つからない場合
     * @throws InsufficientStockException 在庫が不足している場合
     */
    @Transactional
    public OrderResult create(CreateOrder createOrder) {
        Customer customer =
                customerRepository
                        .findById(createOrder.customerId())
                        .orElseThrow(() -> new CustomerNotFoundException(createOrder.customerId()));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
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

        CustomerOrder saved = orderRepository.save(order);
        cartService.clearCart(customer.getId());
        log.info(
                "Order created orderId={} customerId={} totalAmount={}",
                saved.getId(),
                saved.getCustomer().getId(),
                saved.getTotalAmount());
        return toOrderResult(saved);
    }

    /**
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合
     */
    @Transactional
    public OrderResult updateStatus(Long id, OrderStatus newStatus, int version) {
        CustomerOrder order =
                orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(newStatus);
        order.setVersion(version);
        log.info("Order status updated orderId={} status={}", id, newStatus);
        return toOrderResult(orderRepository.save(order));
    }

    /**
     * JOIN FETCH 済みの Product を直接更新します（N+1 回避、JPA dirty checking で保存されます）。
     *
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合
     */
    @Transactional
    public OrderResult cancel(Long id, int version) {
        CustomerOrder order =
                orderRepository
                        .findByIdWithItems(id)
                        .orElseThrow(() -> new OrderNotFoundException(id));
        order.setStatus(OrderStatus.CANCELLED);
        order.setVersion(version);
        log.info("Order cancelled orderId={}", id);
        order.getItems()
                .forEach(e -> e.getProduct().setStock(e.getProduct().getStock() + e.getQuantity()));
        return toOrderResult(orderRepository.save(order));
    }

    private OrderResult toOrderResult(CustomerOrder customerOrder) {
        return toOrderResult(customerOrder, customerOrder.getItems());
    }

    /**
     * 一覧系クエリでは items コレクションを JOIN FETCH しない（Pageable と併用するとメモリ上でページングされてしまうため）。
     * そのため、注文IDごとに別クエリで取得した明細を引数で受け取る。
     */
    private OrderResult toOrderResult(
            CustomerOrder customerOrder, List<CustomerOrderDetail> items) {
        Customer customer = customerOrder.getCustomer();
        return new OrderResult(
                customerOrder.getId(),
                customer.getId(),
                resolveCustomerName(customer),
                customerOrder.getStatus(),
                customerOrder.getTotalAmount(),
                items.stream()
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

    private Page<OrderResult> toOrderResultPage(Page<CustomerOrder> orders) {
        List<Long> orderIds = orders.getContent().stream().map(CustomerOrder::getId).toList();
        Map<Long, List<CustomerOrderDetail>> itemsByOrderId =
                orderIds.isEmpty()
                        ? Map.of()
                        : orderDetailRepository.findAllByOrderIdIn(orderIds).stream()
                                .collect(Collectors.groupingBy(d -> d.getOrder().getId()));
        return orders.map(o -> toOrderResult(o, itemsByOrderId.getOrDefault(o.getId(), List.of())));
    }

    private String resolveCustomerName(Customer customer) {
        String lastName = customer.getLastName();
        String firstName = customer.getFirstName();
        if (lastName == null && firstName == null) {
            return customer.getEmail();
        }
        return Stream.of(lastName, firstName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
    }
}
