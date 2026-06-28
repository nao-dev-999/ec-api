package com.example.ecapi.service.product;

import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
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

/**
 * 商品サービス
 *
 * <p>商品に関するビジネスロジックを提供します。 商品のCRUD操作、検索機能、および関連するビジネスルールを管理します。 Controller はサービスの呼び出しと HTTP
 * レスポンスへの変換のみを担当します。
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final MessageHelper messageHelper;

    /**
     * 全ての商品を取得します。
     *
     * @return 全ての商品情報のリスト
     */
    public List<ProductResult> findAll() {
        List<Product> products = productRepository.findAll();
        return products.stream().map(this::toProductResult).toList();
    }

    /**
     * 指定されたIDの商品を取得します。
     *
     * @param id 商品ID
     * @return 商品情報 {@link ProductResult}
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     */
    public ProductResult findById(Long id) {
        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                messageHelper.get("product.notFound", id)));
        return toProductResult(product);
    }

    /**
     * 複数の条件（商品名、商品説明、価格）に基づいて商品を検索します。 指定された検索条件はAND条件で結合されます。
     *
     * @param name 商品名（部分一致、大文字小文字無視、nullの場合は条件無視）
     * @param description 商品説明（部分一致、大文字小文字無視、nullの場合は条件無視）
     * @param price 価格（指定値以下、nullの場合は条件無視）
     * @return 検索条件に合致する商品情報のリスト
     */
    public List<ProductResult> searchProducts(String name, String description, BigDecimal price) {
        Specification<Product> spec = ProductSpecification.byCriteria(name, description, price);
        List<Product> products = productRepository.findAll(spec, Sort.by("name").ascending());
        return products.stream().map(this::toProductResult).toList();
    }

    /**
     * 新しい商品を登録します。
     *
     * @param createProduct 登録する商品の情報 {@link CreateProduct}
     * @return 登録された商品情報 {@link ProductResult}
     */
    @Transactional
    public ProductResult create(CreateProduct createProduct) {
        Product product = toProduct(createProduct);
        return toProductResult(productRepository.save(product));
    }

    /**
     * 指定されたIDの商品情報を更新します。 楽観ロックを適用するため、{@code updateProduct} には現在の {@code version} を含める必要があります。
     *
     * @param updateProduct 更新する商品の情報 {@link UpdateProduct} (version フィールドを含む)
     * @return 更新された商品情報 {@link ProductResult}
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合（他のトランザクションによって既に更新されている場合）
     */
    @Transactional
    public ProductResult update(UpdateProduct updateProduct) {
        Product product =
                productRepository
                        .findById(updateProduct.id())
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                messageHelper.get(
                                                        "product.notFound", updateProduct.id())));
        return toProductResult(productRepository.save(toProduct(updateProduct)));
    }

    /**
     * 指定されたIDの商品を削除します。
     *
     * @param id 削除対象の商品ID
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     */
    @Transactional
    public void delete(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ProductNotFoundException(
                    messageHelper.get("product.notFound", id)); // メッセージをプロパティファイルから取得
        }
        productRepository.deleteById(id);
    }

    /**
     * CreateProduct DTO を Product エンティティに変換します。
     *
     * @param createProduct
     * @return
     */
    private Product toProduct(CreateProduct createProduct) {
        Product product = new Product();
        product.setName(createProduct.name());
        product.setDescription(createProduct.description());
        product.setPrice(createProduct.price());
        product.setStock(createProduct.stock());
        return product;
    }

    /**
     * UpdateProduct DTO を Product エンティティに変換します。
     *
     * @param updateProduct
     * @return
     */
    private Product toProduct(UpdateProduct updateProduct) {
        Product product = new Product();
        product.setName(updateProduct.name());
        product.setDescription(updateProduct.description());
        product.setPrice(updateProduct.price());
        product.setStock(updateProduct.stock());
        return product;
    }

    /**
     * Product エンティティを ProductResult DTO に変換します。
     *
     * @param product Product エンティティ
     * @return ProductResult DTO
     */
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
