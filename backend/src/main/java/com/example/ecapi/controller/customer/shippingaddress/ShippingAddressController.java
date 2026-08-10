package com.example.ecapi.controller.customer.shippingaddress;

import com.example.ecapi.controller.customer.shippingaddress.dto.CreateShippingAddressRequest;
import com.example.ecapi.controller.customer.shippingaddress.dto.ShippingAddressResponse;
import com.example.ecapi.controller.customer.shippingaddress.dto.UpdateShippingAddressRequest;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.shippingaddress.ShippingAddressService;
import com.example.ecapi.service.shippingaddress.dto.CreateShippingAddress;
import com.example.ecapi.service.shippingaddress.dto.ShippingAddressResult;
import com.example.ecapi.service.shippingaddress.dto.UpdateShippingAddress;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 配送先住所（住所帳）REST コントローラー（顧客本人のみ、要ログイン）
 *
 * <pre>
 * GET    /api/customer/shipping-addresses       自分の配送先住所一覧（新しい順）
 * GET    /api/customer/shipping-addresses/{id}  配送先住所詳細
 * POST   /api/customer/shipping-addresses       配送先住所を登録（初回登録は自動的にデフォルトになる）
 * PUT    /api/customer/shipping-addresses/{id}  配送先住所を更新（部分更新、楽観ロックのため version が必須）
 * DELETE /api/customer/shipping-addresses/{id}  配送先住所を削除
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/shipping-addresses")
@RequiredArgsConstructor
public class ShippingAddressController {

    private final ShippingAddressService shippingAddressService;

    @GetMapping
    public ResponseEntity<List<ShippingAddressResponse>> list(
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        return ResponseEntity.ok(
                shippingAddressService.list(loginUser.getUserId()).stream()
                        .map(this::toResponse)
                        .toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShippingAddressResponse> get(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long id) {
        return ResponseEntity.ok(toResponse(shippingAddressService.get(id, loginUser.getUserId())));
    }

    @PostMapping
    public ResponseEntity<ShippingAddressResponse> create(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody CreateShippingAddressRequest request) {
        ShippingAddressResult result =
                shippingAddressService.create(
                        new CreateShippingAddress(
                                loginUser.getUserId(),
                                request.recipientName(),
                                request.postalCode(),
                                request.prefecture(),
                                request.city(),
                                request.addressLine1(),
                                request.addressLine2(),
                                request.phoneNumber(),
                                request.isDefault()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShippingAddressResponse> update(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @PathVariable Long id,
            @Valid @RequestBody UpdateShippingAddressRequest request) {
        ShippingAddressResult result =
                shippingAddressService.update(
                        new UpdateShippingAddress(
                                id,
                                loginUser.getUserId(),
                                request.recipientName(),
                                request.postalCode(),
                                request.prefecture(),
                                request.city(),
                                request.addressLine1(),
                                request.addressLine2(),
                                request.phoneNumber(),
                                request.isDefault(),
                                request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long id) {
        shippingAddressService.delete(id, loginUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    private ShippingAddressResponse toResponse(ShippingAddressResult result) {
        return new ShippingAddressResponse(
                result.id(),
                result.recipientName(),
                result.postalCode(),
                result.prefecture(),
                result.city(),
                result.addressLine1(),
                result.addressLine2(),
                result.phoneNumber(),
                result.isDefault(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
