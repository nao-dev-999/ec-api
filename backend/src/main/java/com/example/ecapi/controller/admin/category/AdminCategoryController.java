package com.example.ecapi.controller.admin.category;

import com.example.ecapi.controller.admin.category.dto.AdminCategoryResponse;
import com.example.ecapi.controller.admin.category.dto.CreateCategoryRequest;
import com.example.ecapi.controller.admin.category.dto.UpdateCategoryRequest;
import com.example.ecapi.service.category.CategoryService;
import com.example.ecapi.service.category.dto.CategoryResult;
import com.example.ecapi.service.category.dto.CreateCategory;
import com.example.ecapi.service.category.dto.UpdateCategory;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<AdminCategoryResponse>> getAll() {
        return ResponseEntity.ok(categoryService.findAll().stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AdminCategoryResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(toResponse(categoryService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<AdminCategoryResponse> create(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryResult result = categoryService.create(new CreateCategory(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AdminCategoryResponse> update(
            @PathVariable Long id, @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryResult result =
                categoryService.update(new UpdateCategory(id, request.name(), request.version()));
        return ResponseEntity.ok(toResponse(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private AdminCategoryResponse toResponse(CategoryResult result) {
        return new AdminCategoryResponse(
                result.id(),
                result.name(),
                result.createdAt(),
                result.updatedAt(),
                result.version());
    }
}
