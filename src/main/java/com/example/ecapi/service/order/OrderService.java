package com.example.ecapi.service.order;

import com.example.ecapi.config.MessageHelper;
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
import com.example.ecapi.service.order.mapper.OrderEntityMapper;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 注文サービス
 *
 * <p>注文に関するビジネスロジックを提供します。 「在庫チェック → 注文作成 → 在庫減算」といった一連の処理を1つのトランザクションで実行し、 データの整合性を保ちます。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderEntityMapper orderEntityMapper;
    private final MessageHelper messageHelper;

    /**
     * 全ての注文を取得します。
     *
     * @return 全ての注文のリスト
     */
    public List<OrderResult> findAll() {
        return orderEntityMapper.toOrderResultList(orderRepository.findAll());
    }

    /**
     * 指定されたIDの注文詳細を取得します。 JOIN FETCH を使用して、N+1 問題を回避しています。
     *
     * @param id 注文ID
     * @return 注文詳細 {@link OrderResult}
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     */
    public OrderResult findById(Long id) {
        CustomerOrder order =
                orderRepository
                        .findByIdWithItems(id)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                messageHelper.get("order.notFound", id)));
        return orderEntityMapper.toOrderResult(order);
    }

    /**
     * 新しい注文を作成します。 在庫チェック、在庫減算、注文明細の作成、合計金額の計算をトランザクション内で実行します。
     *
     * @param createOrder 作成する注文の情報 {@link CreateOrder}
     * @return 作成された注文の詳細 {@link OrderResult}
     * @throws ProductNotFoundException 注文に含まれる商品が見つからない場合
     * @throws InsufficientStockException 在庫が不足している場合
     */
    @Transactional
    public OrderResult create(CreateOrder createOrder) {
        CustomerOrder order = new CustomerOrder();
        order.setCustomerName(createOrder.customerName());
        order.setStatus(OrderStatus.PENDING);

        for (CreateOrderItem item : createOrder.items()) {
            Product product =
                    productRepository
                            .findById(item.productId())
                            .orElseThrow(
                                    () ->
                                            new ProductNotFoundException(
                                                    messageHelper.get(
                                                            "product.notFound", item.productId())));
            // 在庫チェック
            if (product.getStock() < item.quantity()) {
                throw new InsufficientStockException(
                        messageHelper.get(
                                "order.insufficientStock",
                                product.getName(),
                                product.getStock(),
                                item.quantity()));
            }
            // 在庫減算
            product.setStock(product.getStock() - item.quantity());

            CustomerOrderDetail detail = new CustomerOrderDetail();
            detail.setOrder(order);
            detail.setProduct(product);
            detail.setQuantity(item.quantity());
            detail.setUnitPrice(product.getPrice()); // 価格取得
            detail.setSubtotal(product.getPrice().multiply(BigDecimal.valueOf(item.quantity())));
            order.getItems().add(detail);
        }

        order.setTotalAmount(
                order.getItems().stream()
                        .map(CustomerOrderDetail::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));

        CustomerOrder savedOrder = orderRepository.save(order);

        return orderEntityMapper.toOrderResult(savedOrder);
    }

    /**
     * 指定された注文のステータスを更新します。 楽観ロックを適用するため、更新前に注文を取得し、他のトランザクションによる変更がないか確認します。
     *
     * @param id 注文ID
     * @param newStatus 新しい注文ステータス {@link OrderStatus}
     * @return 更新された注文の詳細 {@link OrderResult}
     * @throws OrderNotFoundException 指定されたIDの注文が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合（他のトランザクションによって既に更新されている場合）
     */
    @Transactional
    public OrderResult updateStatus(Long id, OrderStatus newStatus) {
        CustomerOrder order =
                orderRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                messageHelper.get("order.notFound", id)));
        order.setStatus(newStatus);
        CustomerOrder saved = orderRepository.save(order);
        return orderEntityMapper.toOrderResult(saved);
    }

    /**
     * 指定された注文をキャンセルします。 現在は未実装です。
     *
     * @param id 注文ID
     * @return キャンセルされた注文の詳細 {@link OrderResult}
     * @throws UnsupportedOperationException この操作が未実装の場合
     */
    @Transactional
    public OrderResult cancel(Long id) {
        CustomerOrder order =
                orderRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new OrderNotFoundException(
                                                messageHelper.get("order.notFound", id)));
        order.setStatus(OrderStatus.CANCELLED);
        CustomerOrder saved = orderRepository.save(order);
        return orderEntityMapper.toOrderResult(saved);
    }
}
