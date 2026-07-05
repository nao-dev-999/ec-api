package com.example.ecapi.service.category;

import com.example.ecapi.entity.Category;
import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.CategoryNameDuplicateException;
import com.example.ecapi.exception.CategoryNotFoundException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.CategoryRepository;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.service.category.dto.CategoryResult;
import com.example.ecapi.service.category.dto.CreateCategory;
import com.example.ecapi.service.category.dto.UpdateCategory;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public List<CategoryResult> findAll() {
        return categoryRepository.findAll().stream().map(this::toCategoryResult).toList();
    }

    public CategoryResult findById(Long id) {
        return categoryRepository
                .findById(id)
                .map(this::toCategoryResult)
                .orElseThrow(() -> new CategoryNotFoundException(id));
    }

    @Transactional
    public CategoryResult create(CreateCategory dto) {
        if (categoryRepository.existsByName(dto.name())) {
            throw new CategoryNameDuplicateException(dto.name());
        }
        Category category = new Category();
        category.setName(dto.name());
        Category saved = categoryRepository.save(category);
        log.info("Category created categoryId={} name={}", saved.getId(), saved.getName());
        return toCategoryResult(saved);
    }

    @Transactional
    public CategoryResult update(UpdateCategory dto) {
        Category category =
                categoryRepository
                        .findById(dto.id())
                        .orElseThrow(() -> new CategoryNotFoundException(dto.id()));
        if (categoryRepository.existsByNameAndIdNot(dto.name(), dto.id())) {
            throw new CategoryNameDuplicateException(dto.name());
        }
        category.setName(dto.name());
        category.setVersion(dto.version());
        Category saved = categoryRepository.save(category);
        log.info("Category updated categoryId={}", dto.id());
        return toCategoryResult(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new CategoryNotFoundException(id);
        }
        categoryRepository.deleteById(id);
        log.info("Category deleted categoryId={}", id);
    }

    public List<CategoryResult> findByProductId(Long productId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        return product.getCategories().stream().map(this::toCategoryResult).toList();
    }

    @Transactional
    public void addCategoryToProduct(Long productId, Long categoryId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        Category category =
                categoryRepository
                        .findById(categoryId)
                        .orElseThrow(() -> new CategoryNotFoundException(categoryId));
        product.getCategories().add(category);
        productRepository.save(product);
        log.info("Category added to product productId={} categoryId={}", productId, categoryId);
    }

    @Transactional
    public void removeCategoryFromProduct(Long productId, Long categoryId) {
        Product product =
                productRepository
                        .findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(productId));
        product.getCategories().removeIf(c -> c.getId().equals(categoryId));
        productRepository.save(product);
        log.info("Category removed from product productId={} categoryId={}", productId, categoryId);
    }

    private CategoryResult toCategoryResult(Category category) {
        return new CategoryResult(
                category.getId(),
                category.getName(),
                LocalDateTime.ofInstant(category.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(category.getUpdatedAt(), ZoneId.systemDefault()),
                category.getVersion());
    }
}
