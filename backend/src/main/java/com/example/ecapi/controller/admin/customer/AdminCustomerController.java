package com.example.ecapi.controller.admin.customer;

import com.example.ecapi.controller.admin.customer.dto.AdminCustomerResponse;
import com.example.ecapi.service.customer.CustomerService;
import com.example.ecapi.service.customer.dto.CustomerResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final CustomerService customerService;

    @GetMapping
    public ResponseEntity<List<AdminCustomerResponse>> getAll() {
        return ResponseEntity.ok(customerService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCustomerResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(customerService.findById(id)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminCustomerResponse toResponse(CustomerResult result) {
        return new AdminCustomerResponse(
                result.id(),
                result.email(),
                result.lastName(),
                result.firstName(),
                result.lastNameKana(),
                result.firstNameKana(),
                result.phoneNumber(),
                result.postalCode(),
                result.prefecture(),
                result.city(),
                result.addressLine1(),
                result.addressLine2(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
