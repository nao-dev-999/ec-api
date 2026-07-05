package com.example.ecapi.controller.customer.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.controller.customer.order.dto.OrderItemResponse;
import com.example.ecapi.controller.customer.order.dto.OrderRequest;
import com.example.ecapi.controller.customer.order.dto.OrderResponse;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.CreateOrderItem;
import com.example.ecapi.service.order.dto.OrderResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 注文 REST コントローラー
 *
 * <p>注文に関するCRUD操作とステータス更新機能を提供するRESTful API。
 *
 * <pre>
 * GET   /api/orders                      全注文取得
 * GET   /api/orders/{id}                 注文詳細
 * POST  /api/orders                      注文作成（在庫チェックあり）
 * PATCH /api/orders/{id}/status          ステータス更新
 * </pre>
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    /**
     * ログイン中の顧客自身の注文を新しい順にページング取得します。
     *
     * @return 注文のページ {@link OrderResponse}
     */
    @GetMapping
    public ResponseEntity<PageResponse<OrderResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "orderedAt"));
        Page<OrderResponse> result =
                orderService
                        .findAllByCustomerId(loginUser.getUserId(), pageable)
                        .map(this::toOrderResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    /**
     * 指定されたIDの注文詳細を取得します。ログイン中の顧客自身の注文でない場合は404を返します。
     *
     * @param id 注文ID
     * @return 注文詳細 {@link OrderResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(
            @PathVariable Long id, @AuthenticationPrincipal LoginUserDetails loginUser) {
        return ResponseEntity.ok(
                toOrderResponse(orderService.findByIdForCustomer(id, loginUser.getUserId())));
    }

    /**
     * 新しい注文を作成します。注文者はログイン中の顧客自身になります。
     *
     * @param request 作成する注文の情報 {@link OrderRequest}
     * @return 作成された注文の詳細 {@link OrderResponse}
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(
            @Valid @RequestBody OrderRequest request,
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        OrderResult result = orderService.create(toCreateOrder(request, loginUser.getUserId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toOrderResponse(result));
    }

    /**
     * 指定された注文のステータスを更新します。ログイン中の顧客自身の注文でない場合は404を返します。
     * サービス層で楽観ロックが適用され、他のトランザクションによる変更があった場合は競合エラーが発生する可能性があります。
     *
     * @param id 注文ID
     * @param status 新しい注文ステータス {@link OrderStatus}
     * @return 更新された注文の詳細 {@link OrderResponse}
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam OrderStatus status,
            @RequestParam int version,
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        orderService.findByIdForCustomer(id, loginUser.getUserId());
        OrderResult result =
                switch (status) {
                    case PENDING, CONFIRMED, SHIPPED, DELIVERED ->
                            orderService.updateStatus(id, status, version);
                    case CANCELLED -> orderService.cancel(id, version);
                };
        return ResponseEntity.ok(toOrderResponse(result));
    }

    private OrderResponse toOrderResponse(OrderResult result) {
        return new OrderResponse(
                result.id(),
                result.customerId(),
                result.customerName(),
                result.status(),
                result.totalAmount(),
                result.items().stream()
                        .map(
                                i ->
                                        new OrderItemResponse(
                                                i.productId(),
                                                i.productName(),
                                                i.quantity(),
                                                i.unitPrice(),
                                                i.subtotal()))
                        .toList(),
                result.orderedAt(),
                result.updatedAt(),
                result.version());
    }

    private CreateOrder toCreateOrder(OrderRequest request, Long customerId) {
        return new CreateOrder(
                customerId,
                request.items().stream()
                        .map(item -> new CreateOrderItem(item.productId(), item.quantity()))
                        .toList());
    }
}
