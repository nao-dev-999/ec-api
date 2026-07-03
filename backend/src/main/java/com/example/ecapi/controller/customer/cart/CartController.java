package com.example.ecapi.controller.customer.cart;

import com.example.ecapi.controller.customer.cart.dto.AddCartItemRequest;
import com.example.ecapi.controller.customer.cart.dto.CartItemResponse;
import com.example.ecapi.controller.customer.cart.dto.UpdateCartItemQuantityRequest;
import com.example.ecapi.service.auth.LoginUserDetails;
import com.example.ecapi.service.cart.CartService;
import com.example.ecapi.service.cart.dto.AddCartItem;
import com.example.ecapi.service.cart.dto.CartItemResult;
import com.example.ecapi.service.cart.dto.UpdateCartItemQuantity;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCart(
            @AuthenticationPrincipal LoginUserDetails loginUser) {
        return ResponseEntity.ok(
                cartService.getCart(loginUser.getUserId()).stream().map(this::toResponse).toList());
    }

    @PostMapping("/items")
    public ResponseEntity<CartItemResponse> addItem(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @Valid @RequestBody AddCartItemRequest request) {
        CartItemResult result =
                cartService.addItem(
                        loginUser.getUserId(),
                        new AddCartItem(request.productId(), request.quantity()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PatchMapping("/items/{productId}")
    public ResponseEntity<CartItemResponse> updateQuantity(
            @AuthenticationPrincipal LoginUserDetails loginUser,
            @PathVariable Long productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        CartItemResult result =
                cartService.updateQuantity(
                        loginUser.getUserId(),
                        new UpdateCartItemQuantity(
                                productId, request.quantity(), request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal LoginUserDetails loginUser, @PathVariable Long productId) {
        cartService.removeItem(loginUser.getUserId(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal LoginUserDetails loginUser) {
        cartService.clearCart(loginUser.getUserId());
        return ResponseEntity.noContent().build();
    }

    private CartItemResponse toResponse(CartItemResult result) {
        return new CartItemResponse(
                result.id(),
                result.productId(),
                result.productName(),
                result.unitPrice(),
                result.quantity(),
                result.subtotal(),
                result.version());
    }
}
