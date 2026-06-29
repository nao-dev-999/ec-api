package com.example.ecapi.controller.admin.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.admin.order.dto.AdminOrderItemResponse;
import com.example.ecapi.controller.admin.order.dto.AdminOrderResponse;
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
    public ResponseEntity<List<AdminOrderResponse>> getAll() {
        return ResponseEntity.ok(
                orderService.findAll().stream().map(this::toAdminOrderResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toAdminOrderResponse(orderService.findById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateStatus(
            @PathVariable Long id, @RequestParam OrderStatus status) {
        OrderResult result =
                switch (status) {
                    case PENDING, CONFIRMED, SHIPPED, DELIVERED ->
                            orderService.updateStatus(id, status);
                    case CANCELLED -> orderService.cancel(id);
                };
        return ResponseEntity.ok(toAdminOrderResponse(result));
    }

    private AdminOrderResponse toAdminOrderResponse(OrderResult result) {
        return new AdminOrderResponse(
                result.id(),
                result.customerName(),
                result.status(),
                result.totalAmount(),
                result.items().stream()
                        .map(
                                i ->
                                        new AdminOrderItemResponse(
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
