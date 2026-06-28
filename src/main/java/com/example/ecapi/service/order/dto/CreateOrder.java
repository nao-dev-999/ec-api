package com.example.ecapi.service.order.dto;

import java.util.List;

public record CreateOrder(String customerId, String customerName, List<CreateOrderItem> items) {}
