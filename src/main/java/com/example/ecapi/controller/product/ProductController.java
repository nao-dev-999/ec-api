package com.example.ecapi.controller.product;

import com.example.ecapi.controller.product.dto.CreateProductRequest;
import com.example.ecapi.controller.product.dto.ProductResponse;
import com.example.ecapi.controller.product.dto.UpdateProductRequest;
import com.example.ecapi.controller.product.mapper.ProductApiMapper;
import com.example.ecapi.service.product.ProductService;
import com.example.ecapi.service.product.dto.ProductResult;
import jakarta.validation.Valid;
import java.math.BigDecimal; // BigDecimal をインポート
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 商品 REST コントローラー
 *
 * <p>商品に関するCRUD操作と検索機能を提供するRESTful API。
 *
 * <pre>
 * GET    /api/products                      全商品取得、または検索条件に合致する商品を取得
 * GET    /api/products?name=xxx             商品名で部分一致検索（大文字小文字無視）
 * GET    /api/products?description=xxx      商品説明で部分一致検索（大文字小文字無視）
 * GET    /api/products?price=xxx            価格が指定値以下の商品を検索
 *                                         （name, description, price はAND条件で検索）
 * GET    /api/products/{id}                 商品詳細
 * POST   /api/products                      商品登録
 * PUT    /api/products/{id}                 商品更新
 * DELETE /api/products/{id}                 商品削除
 * </pre>
 */
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductApiMapper productApiMapper;

    /**
     * 全商品を取得、または検索条件に合致する商品を取得します。 検索条件が複数指定された場合はAND条件で検索されます。
     *
     * @param name 商品名（部分一致、大文字小文字無視）
     * @param description 商品説明（部分一致、大文字小文字無視）
     * @param price 価格（指定値以下）
     * @return 検索結果の商品リスト
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAll(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) BigDecimal price) {
        name = name == null ? null : name.trim();
        description = description == null ? null : description.trim();
        List<ProductResult> results = productService.searchProducts(name, description, price);
        return ResponseEntity.ok(productApiMapper.toProductResponseList(results));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productApiMapper.toProductResponse(productService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResult result = productService.create(productApiMapper.toCreateProduct(request));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(productApiMapper.toProductResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        ProductResult result = productService.update(id, productApiMapper.toUpdateProduct(request));
        return ResponseEntity.ok(productApiMapper.toProductResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
