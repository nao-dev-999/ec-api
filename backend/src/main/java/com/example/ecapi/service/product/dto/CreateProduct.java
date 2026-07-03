package com.example.ecapi.service.product.dto;

import java.math.BigDecimal;

public record CreateProduct(String name, String description, BigDecimal price, int stock) {}
