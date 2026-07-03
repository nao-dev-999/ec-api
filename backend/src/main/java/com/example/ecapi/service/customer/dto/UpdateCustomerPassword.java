package com.example.ecapi.service.customer.dto;

public record UpdateCustomerPassword(String currentPassword, String newPassword, int version) {}
