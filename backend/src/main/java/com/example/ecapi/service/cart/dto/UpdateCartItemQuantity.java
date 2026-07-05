package com.example.ecapi.service.cart.dto;

public record UpdateCartItemQuantity(Long productId, int quantity, int version) {}
