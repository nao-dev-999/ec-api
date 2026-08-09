package com.example.ecapi.service.wishlist;

import com.example.ecapi.entity.Product;
import com.example.ecapi.entity.WishlistItem;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.repository.WishlistItemRepository;
import com.example.ecapi.service.wishlist.dto.WishlistItemResult;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;

    public List<WishlistItemResult> getWishlist(Long customerId) {
        List<WishlistItem> items =
                wishlistItemRepository.findAllByCustomerIdOrderByCreatedAtDesc(customerId);
        Map<Long, Product> productsById =
                productRepository
                        .findAllById(items.stream().map(WishlistItem::getProductId).toList())
                        .stream()
                        .collect(Collectors.toMap(Product::getId, Function.identity()));

        return items.stream()
                .map(
                        item -> {
                            Product product = productsById.get(item.getProductId());
                            if (product == null) {
                                throw new ProductNotFoundException(item.getProductId());
                            }
                            return toResult(item, product);
                        })
                .toList();
    }

    /** すでにお気に入り登録済みの場合は何もせず既存の登録内容を返す（冪等）。 */
    @Transactional
    public WishlistItemResult addItem(Long customerId, Long productId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));

        WishlistItem item =
                wishlistItemRepository
                        .findByCustomerIdAndProductId(customerId, productId)
                        .orElseGet(
                                () -> {
                                    WishlistItem newItem = new WishlistItem();
                                    newItem.setCustomerId(customerId);
                                    newItem.setProductId(productId);
                                    return wishlistItemRepository.save(newItem);
                                });
        log.info("WishlistItem added customerId={} productId={}", customerId, productId);
        return toResult(item, product);
    }

    /** 登録されていない商品を指定した場合も含め、冪等に削除する。 */
    @Transactional
    public void removeItem(Long customerId, Long productId) {
        wishlistItemRepository.deleteByCustomerIdAndProductId(customerId, productId);
        log.info("WishlistItem removed customerId={} productId={}", customerId, productId);
    }

    private WishlistItemResult toResult(WishlistItem item, Product product) {
        return new WishlistItemResult(
                item.getId(),
                item.getProductId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                LocalDateTime.ofInstant(item.getCreatedAt(), ZoneId.systemDefault()));
    }
}
