package com.example.ecapi.service.cart;

import com.example.ecapi.entity.CartItem;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.CartItemNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CartItemRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.cart.dto.AddCartItem;
import com.example.ecapi.service.cart.dto.CartItemResult;
import com.example.ecapi.service.cart.dto.UpdateCartItemQuantity;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public List<CartItemResult> getCart(Long customerId) {
        return cartItemRepository.findAllByCustomerId(customerId).stream()
                .map(
                        item -> {
                            Product product =
                                    productRepository
                                            .findById(item.getProductId())
                                            .orElseThrow(
                                                    () ->
                                                            new ProductNotFoundException(
                                                                    item.getProductId()));
                            return toResult(item, product);
                        })
                .toList();
    }

    @Transactional
    public CartItemResult addItem(Long customerId, AddCartItem dto) {
        Product product =
                productRepository
                        .findById(dto.productId())
                        .orElseThrow(() -> new ProductNotFoundException(dto.productId()));

        CartItem item =
                cartItemRepository
                        .findByCustomerIdAndProductId(customerId, dto.productId())
                        .orElseGet(
                                () -> {
                                    CartItem newItem = new CartItem();
                                    newItem.setCustomerId(customerId);
                                    newItem.setProductId(dto.productId());
                                    newItem.setQuantity(0);
                                    return newItem;
                                });

        item.setQuantity(item.getQuantity() + dto.quantity());
        CartItem saved = cartItemRepository.save(item);
        log.info(
                "CartItem added customerId={} productId={} quantity={}",
                customerId,
                dto.productId(),
                dto.quantity());
        return toResult(saved, product);
    }

    @Transactional
    public CartItemResult updateQuantity(Long customerId, UpdateCartItemQuantity dto) {
        CartItem item =
                cartItemRepository
                        .findByCustomerIdAndProductId(customerId, dto.productId())
                        .orElseThrow(() -> new CartItemNotFoundException(dto.productId()));
        Product product =
                productRepository
                        .findById(dto.productId())
                        .orElseThrow(() -> new ProductNotFoundException(dto.productId()));

        item.setQuantity(dto.quantity());
        item.setVersion(dto.version());
        CartItem saved = cartItemRepository.save(item);
        log.info(
                "CartItem quantity updated customerId={} productId={} quantity={}",
                customerId,
                dto.productId(),
                dto.quantity());
        return toResult(saved, product);
    }

    @Transactional
    public void removeItem(Long customerId, Long productId) {
        CartItem item =
                cartItemRepository
                        .findByCustomerIdAndProductId(customerId, productId)
                        .orElseThrow(() -> new CartItemNotFoundException(productId));
        cartItemRepository.delete(item);
        log.info("CartItem removed customerId={} productId={}", customerId, productId);
    }

    @Transactional
    public void clearCart(Long customerId) {
        cartItemRepository.deleteByCustomerId(customerId);
        log.info("Cart cleared customerId={}", customerId);
    }

    private CartItemResult toResult(CartItem item, Product product) {
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return new CartItemResult(
                item.getId(),
                item.getCustomerId(),
                item.getProductId(),
                product.getName(),
                product.getPrice(),
                item.getQuantity(),
                subtotal,
                item.getVersion());
    }
}
