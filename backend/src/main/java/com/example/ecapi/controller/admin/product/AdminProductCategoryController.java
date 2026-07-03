package com.example.ecapi.controller.admin.product;

import com.example.ecapi.controller.admin.category.dto.AdminCategoryResponse;
import com.example.ecapi.service.category.CategoryService;
import com.example.ecapi.service.category.dto.CategoryResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products/{productId}/categories")
@RequiredArgsConstructor
public class AdminProductCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<AdminCategoryResponse>> getCategories(@PathVariable Long productId) {
        List<CategoryResult> results = categoryService.findByProductId(productId);
        return ResponseEntity.ok(
                results.stream()
                        .map(
                                r ->
                                        new AdminCategoryResponse(
                                                r.id(),
                                                r.name(),
                                                r.createdAt(),
                                                r.updatedAt(),
                                                r.version()))
                        .toList());
    }

    @PostMapping("/{categoryId}")
    public ResponseEntity<Void> addCategory(
            @PathVariable Long productId, @PathVariable Long categoryId) {
        categoryService.addCategoryToProduct(productId, categoryId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> removeCategory(
            @PathVariable Long productId, @PathVariable Long categoryId) {
        categoryService.removeCategoryFromProduct(productId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
