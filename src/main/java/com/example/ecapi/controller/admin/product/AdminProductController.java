package com.example.ecapi.controller.admin.product;

import com.example.ecapi.controller.customer.product.dto.CreateProductRequest;
import com.example.ecapi.controller.customer.product.dto.ProductResponse;
import com.example.ecapi.controller.customer.product.dto.UpdateProductRequest;
import com.example.ecapi.service.product.ProductService;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductService productService;

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
        return ResponseEntity.ok(results.stream().map(this::toProductResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toProductResponse(productService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {
        ProductResult result = productService.create(toCreateProduct(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(toProductResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProductResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateProductRequest request) {
        if (!id.equals(request.id())) {
            throw new IllegalArgumentException("Path variable id and request body id must match.");
        }
        ProductResult result = productService.update(toUpdateProduct(request));
        return ResponseEntity.ok(toProductResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * ProductResultをProductResponseに変換します。
     *
     * @param result
     * @return
     */
    private ProductResponse toProductResponse(ProductResult result) {
        return new ProductResponse(
                result.id(),
                result.name(),
                result.description(),
                result.price(),
                result.stock(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }

    /**
     * CreateProductRequestをCreateProductに変換します。
     *
     * @param request
     * @return
     */
    private CreateProduct toCreateProduct(CreateProductRequest request) {
        return new CreateProduct(
                request.name(), request.description(), request.price(), request.stock());
    }

    /**
     * UpdateProductRequestをUpdateProductに変換します。
     *
     * @param request
     * @return
     */
    private UpdateProduct toUpdateProduct(UpdateProductRequest request) {
        return new UpdateProduct(
                request.id(),
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.version());
    }
}
