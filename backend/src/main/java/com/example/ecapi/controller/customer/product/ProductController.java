package com.example.ecapi.controller.customer.product;

import com.example.ecapi.controller.common.dto.PageResponse;
import com.example.ecapi.controller.customer.product.dto.ProductResponse;
import com.example.ecapi.service.product.ProductService;
import com.example.ecapi.service.product.dto.ProductResult;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 商品 REST コントローラー（顧客向け参照専用）
 *
 * <p>商品の参照・検索機能を提供するRESTful API。
 *
 * <pre>
 * GET    /api/customer/products                      全商品取得、または検索条件に合致する商品を取得（ページング）
 * GET    /api/customer/products?name=xxx             商品名で部分一致検索（大文字小文字無視）
 * GET    /api/customer/products?description=xxx      商品説明で部分一致検索（大文字小文字無視）
 * GET    /api/customer/products?price=xxx            価格が指定値以下の商品を検索
 *                                                   （name, description, price はAND条件で検索）
 * GET    /api/customer/products?page=0&amp;size=20      ページ番号（0始まり）・件数を指定
 * GET    /api/customer/products/{id}                 商品詳細
 * </pre>
 */
@RestController
@RequestMapping("/api/customer/products")
@RequiredArgsConstructor
public class ProductController {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductService productService;

    /**
     * 全商品を取得、または検索条件に合致する商品を新しい順にページング取得します。 検索条件が複数指定された場合はAND条件で検索されます。
     *
     * @param name 商品名（部分一致、大文字小文字無視）
     * @param description 商品説明（部分一致、大文字小文字無視）
     * @param price 価格（指定値以下）
     * @return 検索結果の商品ページ {@link ProductResponse}
     */
    @GetMapping
    public ResponseEntity<PageResponse<ProductResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
        Pageable pageable =
                PageRequest.of(
                        Math.max(page, 0),
                        Math.clamp(size, 1, MAX_PAGE_SIZE),
                        Sort.by("name").ascending());
        Page<ProductResponse> result =
                productService
                        .searchProducts(name, description, price, pageable)
                        .map(this::toProductResponse);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toProductResponse(productService.findById(id)));
    }

    private ProductResponse toProductResponse(ProductResult result) {
        return new ProductResponse(
                result.id(),
                result.name(),
                result.description(),
                result.price(),
                result.stock(),
                result.imageUrl(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
