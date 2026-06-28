package com.example.ecapi.controller.admin.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.customer.order.dto.OrderItemResponse;
import com.example.ecapi.controller.customer.order.dto.OrderResponse;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.OrderResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private final OrderService orderService;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll() {
        List<OrderResult> results = orderService.findAll();
        List<OrderResponse> response = results.stream().map(this::toOrderResponse).toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toOrderResponse(orderService.findById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<OrderResponse> updateStatus(
            @PathVariable Long id, @RequestParam OrderStatus status) {
        OrderResult result =
                switch (status) {
                    case PENDING, CONFIRMED, SHIPPED, DELIVERED ->
                            orderService.updateStatus(id, status);
                    case CANCELLED -> orderService.cancel(id);
                };
        return ResponseEntity.ok(toOrderResponse(result));
    }

    /**
     * Convert OrderResult to OrderResponse
     *
     * @param result
     * @return
     */
    private OrderResponse toOrderResponse(OrderResult result) {
        return new OrderResponse(
                result.id(),
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
}
