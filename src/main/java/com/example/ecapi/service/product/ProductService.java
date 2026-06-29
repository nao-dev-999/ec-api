package com.example.ecapi.service.product;

import com.example.ecapi.entity.Product;
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
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public List<ProductResult> searchProducts(String name, String description, BigDecimal price) {
        Specification<Product> spec = ProductSpecification.byCriteria(name, description, price);
        return productRepository.findAll(spec, Sort.by("name").ascending()).stream()
                .map(this::toProductResult)
                .toList();
    }

    @Transactional
    public ProductResult create(CreateProduct createProduct) {
        return toProductResult(productRepository.save(toProduct(createProduct)));
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
        product.setVersion(updateProduct.version());
        return toProductResult(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(id);
        }
        productRepository.deleteById(id);
    }

    private Product toProduct(CreateProduct createProduct) {
        Product product = new Product();
        product.setName(createProduct.name());
        product.setDescription(createProduct.description());
        product.setPrice(createProduct.price());
        product.setStock(createProduct.stock());
        return product;
    }

    private ProductResult toProductResult(Product product) {
        return new ProductResult(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                LocalDateTime.ofInstant(product.getCreatedAt(), ZoneId.systemDefault()),
                LocalDateTime.ofInstant(product.getUpdatedAt(), ZoneId.systemDefault()),
                product.getVersion());
    }
}
