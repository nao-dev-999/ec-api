package com.example.ecapi.service.product;

import com.example.ecapi.entity.Product;
import com.example.ecapi.exception.ProductNotFoundException;
import com.example.ecapi.helper.MessageHelper;
import com.example.ecapi.repository.ProductRepository;
import com.example.ecapi.repository.ProductSpecification;
import com.example.ecapi.service.product.dto.CreateProduct;
import com.example.ecapi.service.product.dto.ProductResult;
import com.example.ecapi.service.product.dto.UpdateProduct;
import com.example.ecapi.service.product.mapper.ProductEntityMapper;
import jakarta.persistence.OptimisticLockException;
import java.math.BigDecimal;
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
    private final ProductEntityMapper productEntityMapper;
    private final MessageHelper messageHelper;

    /**
     * 全ての商品を取得します。
     *
     * @return 全ての商品情報のリスト
     */
    public List<ProductResult> findAll() {
        List<Product> products = productRepository.findAll();
        return productEntityMapper.toProductResultList(products);
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
                                                messageHelper.get(
                                                        "product.notFound",
                                                        id))); // メッセージをプロパティファイルから取得
        return productEntityMapper.toProductResult(product);
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
        return productEntityMapper.toProductResultList(products);
    }

    /**
     * 新しい商品を登録します。
     *
     * @param createProduct 登録する商品の情報 {@link CreateProduct}
     * @return 登録された商品情報 {@link ProductResult}
     */
    @Transactional
    public ProductResult create(CreateProduct createProduct) {
        Product product = productEntityMapper.toProduct(createProduct);
        return productEntityMapper.toProductResult(productRepository.save(product));
    }

    /**
     * 指定されたIDの商品情報を更新します。 楽観ロックを適用するため、{@code updateProduct} には現在の {@code version} を含める必要があります。
     *
     * @param id 更新対象の商品ID
     * @param updateProduct 更新する商品の情報 {@link UpdateProduct} (version フィールドを含む)
     * @return 更新された商品情報 {@link ProductResult}
     * @throws ProductNotFoundException 指定されたIDの商品が見つからない場合
     * @throws OptimisticLockException 楽観ロックの競合が発生した場合（他のトランザクションによって既に更新されている場合）
     */
    @Transactional
    public ProductResult update(Long id, UpdateProduct updateProduct) {
        Product product =
                productRepository
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new ProductNotFoundException(
                                                messageHelper.get(
                                                        "product.notFound",
                                                        id))); // メッセージをプロパティファイルから取得
        productEntityMapper.updateProductFromUpdate(updateProduct, product);
        return productEntityMapper.toProductResult(productRepository.save(product));
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
}
