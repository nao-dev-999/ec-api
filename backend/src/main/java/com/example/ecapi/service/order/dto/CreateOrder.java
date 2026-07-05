package com.example.ecapi.service.order.dto;

import java.util.List;

public record CreateOrder(Long customerId, List<CreateOrderItem> items) {}
