package com.example.ecapi.controller.admin.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.admin.order.dto.AdminOrderItemResponse;
import com.example.ecapi.controller.admin.order.dto.AdminOrderResponse;
import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.service.order.OrderService;
import com.example.ecapi.service.order.dto.OrderResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    private static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    /** 全顧客の注文を新しい順にページング取得します。 */
    @GetMapping
    public ResponseEntity<PageResponse<AdminOrderResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by(Sort.Direction.DESC, "orderedAt"));
        Page<AdminOrderResponse> result =
                orderService.findAll(pageable).map(this::toAdminOrderResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminOrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toAdminOrderResponse(orderService.findById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<AdminOrderResponse> updateStatus(
            @PathVariable Long id, @RequestParam OrderStatus status, @RequestParam int version) {
        OrderResult result =
                switch (status) {
                    case PENDING, CONFIRMED, SHIPPED, DELIVERED ->
                            orderService.updateStatus(id, status, version);
                    case CANCELLED -> orderService.cancel(id, version);
                };
        return ResponseEntity.ok(toAdminOrderResponse(result));
    }

    private AdminOrderResponse toAdminOrderResponse(OrderResult result) {
        return new AdminOrderResponse(
                result.id(),
                result.customerId(),
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
