package com.example.ecapi.controller.order;

import static com.example.ecapi.constant.OrderStatus.*;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.order.dto.OrderRequest;
import com.example.ecapi.controller.order.dto.OrderResponse;
import com.example.ecapi.controller.order.mapper.OrderApiMapper;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.CreateOrder;
import com.example.ecapi.service.order.dto.OrderResult;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    private final OrderService orderService;
    private final OrderApiMapper orderApiMapper;

    /**
     * 全ての注文を取得します。
     *
     * @return 全ての注文のリスト {@link OrderResponse}
     */
    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll() {
        List<OrderResult> result = orderService.findAll();
        List<OrderResponse> responses = orderApiMapper.toOrderResponseList(result);
        return ResponseEntity.ok(responses);
    }

    /**
     * 指定されたIDの注文詳細を取得します。
     *
     * @param id 注文ID
     * @return 注文詳細 {@link OrderResponse}
     */
    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        OrderResult result = orderService.findById(id);
        return ResponseEntity.ok(orderApiMapper.toOrderResponse(result));
    }

    /**
     * 新しい注文を作成します。
     *
     * @param request 作成する注文の情報 {@link OrderRequest}
     * @return 作成された注文の詳細 {@link OrderResponse}
     */
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody OrderRequest request) {
        CreateOrder createOrder = orderApiMapper.toCreateOrder(request);
        OrderResult result = orderService.create(createOrder);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderApiMapper.toOrderResponse(result));
    }

    /**
     * 指定された注文のステータスを更新します。 サービス層で楽観ロックが適用され、他のトランザクションによる変更があった場合は競合エラーが発生する可能性があります。
     *
     * @param id 注文ID
     * @param status 新しい注文ステータス {@link OrderStatus}
     * @return 更新された注文の詳細 {@link OrderResponse}
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id, @RequestParam OrderStatus status) {
        OrderResult result =
                switch (status) {
                    case PENDING, CONFIRMED, SHIPPED, DELIVERED ->
                            orderService.updateStatus(id, status);
                    case CANCELLED -> orderService.cancel(id);
                };
        return ResponseEntity.ok(orderApiMapper.toOrderResponse(result));
    }
}
