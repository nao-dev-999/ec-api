package com.example.ecapi.service.product;

import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.ProductInUseException;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.repository.ProductSpecification;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public List<ProductResult> findAll() {
        return productRepository.findAll().stream().map(this::toProductResult).toList();
    }

    public ProductResult findById(Long id) {
        return productRepository
                .findById(id)
                .map(this::toProductResult)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    /** 管理画面向け: 在庫が閾値以下の商品を在庫の少ない順に取得（低在庫アラート） */
    public List<ProductResult> getLowStockProducts(int threshold) {
        return productRepository
                .findByDeletedFalseAndStockLessThanEqualOrderByStockAsc(threshold)
                .stream()
                .map(this::toProductResult)
                .toList();
    }

    public Page<ProductResult> searchProducts(
            String name, String description, BigDecimal price, Pageable pageable) {
        Specification<Product> spec =
                ProductSpecification.byCriteria(name, description, price)
                        .and(ProductSpecification.notDeleted());
        return productRepository.findAll(spec, pageable).map(this::toProductResult);
    }

    @Transactional
    public ProductResult create(CreateProduct createProduct) {
        Product saved = productRepository.save(toProduct(createProduct));
        log.info("Product created productId={} name={}", saved.getId(), saved.getName());
        return toProductResult(saved);
    }

    /**
     * 楽観ロックを適用するため、{@code updateProduct} には現在の {@code version} を含める必要があります。
     *
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合
     */
    @Transactional
    public ProductResult update(UpdateProduct updateProduct) {
        Product product =
                productRepository
                        .findById(updateProduct.id())
                        .orElseThrow(() -> new ProductNotFoundException(updateProduct.id()));
        if (updateProduct.name() != null) product.setName(updateProduct.name());
        if (updateProduct.description() != null)
            product.setDescription(updateProduct.description());
        if (updateProduct.price() != null) product.setPrice(updateProduct.price());
        if (updateProduct.stock() != null) product.setStock(updateProduct.stock());
        if (updateProduct.imageUrl() != null) product.setImageUrl(updateProduct.imageUrl());
        product.setVersion(updateProduct.version());
        log.info("Product updated productId={}", updateProduct.id());
        return toProductResult(productRepository.save(product));
    }

    /**
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     * @throws ProductInUseException 商品が注文で参照されているため削除できない場合
     */
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        try {
            productRepository.deleteById(id);
            productRepository.flush();
        } catch (DataIntegrityViolationException _) {
            throw new ProductInUseException(id);
        }
        log.info("Product deleted productId={}", id);
    }

    private Product toProduct(CreateProduct createProduct) {
        Product product = new Product();
        product.setName(createProduct.name());
        product.setDescription(createProduct.description());
        product.setPrice(createProduct.price());
        product.setStock(createProduct.stock());
        product.setImageUrl(createProduct.imageUrl());
        return product;
    }

    private ProductResult toProductResult(Product product) {
        return new ProductResult(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getImageUrl(),
                LocalDateTime.ofInstant(product.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(product.getUpdatedAt(), ZoneId.systemDefault()),
                product.getVersion());
    }
}
