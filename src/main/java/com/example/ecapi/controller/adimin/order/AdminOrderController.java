package com.example.ecapi.controller.adimin.order;

import com.example.ecapi.constant.OrderStatus;
import com.example.ecapi.controller.customer.order.dto.OrderResponse;
import com.example.ecapi.controller.customer.order.mapper.OrderApiMapper;
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
    private final OrderApiMapper orderApiMapper;

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getAll() {
        List<OrderResult> result = orderService.findAll();
        return ResponseEntity.ok(orderApiMapper.toOrderResponseList(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(orderApiMapper.toOrderResponse(orderService.findById(id)));
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
        return ResponseEntity.ok(orderApiMapper.toOrderResponse(result));
    }
}
