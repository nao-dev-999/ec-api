package com.example.ecapi.controller.customer.me;

import com.example.ecapi.controller.customer.me.dto.CustomerMeResponse;
import com.example.ecapi.controller.customer.me.dto.UpdateEmailRequest;
import com.example.ecapi.controller.customer.me.dto.UpdatePasswordRequest;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.customer.CustomerMeService;
import com.example.ecapi.service.customer.dto.CustomerResult;
import com.example.ecapi.service.customer.dto.UpdateCustomerEmail;
import com.example.ecapi.service.customer.dto.UpdateCustomerPassword;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/me")
@RequiredArgsConstructor
public class CustomerMeController {

    private final CustomerMeService customerMeService;

    @GetMapping
    public ResponseEntity<CustomerMeResponse> getMe(
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        return ResponseEntity.ok(toResponse(customerMeService.findMe(loginUser.getUserId())));
    }

    @PatchMapping("/email")
    public ResponseEntity<CustomerMeResponse> updateEmail(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody UpdateEmailRequest request) {
        CustomerResult result =
                customerMeService.updateEmail(
                        loginUser.getUserId(),
                        new UpdateCustomerEmail(request.email(), request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @PatchMapping("/password")
    public ResponseEntity<Void> updatePassword(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody UpdatePasswordRequest request) {
        customerMeService.updatePassword(
                loginUser.getUserId(),
                new UpdateCustomerPassword(
                        request.currentPassword(), request.newPassword(), request.version()));
        return ResponseEntity.noContent().build();
    }

    private CustomerMeResponse toResponse(CustomerResult result) {
        return new CustomerMeResponse(
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
