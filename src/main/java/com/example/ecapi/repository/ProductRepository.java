package com.example.ecapi.repository;

import com.example.ecapi.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 商品リポジトリ
 *
 * <p>Spring Data JPA がメソッド名・@Query を元にクエリを自動生成する。
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // 商品名のあいまい検索（大文字小文字無視）
    List<Product> findByNameContainingIgnoreCase(String keyword);

    // 商品説明のあいまい検索（大文字小文字無視）
    List<Product> findByDescriptionContainingIgnoreCase(String keyword);

    // 価格で指定された値以下の商品を検索
    List<Product> findByPriceLessThanEqual(BigDecimal price);

    // 在庫あり商品のみ取得
    List<Product> findByStockGreaterThan(int minStock);

    // 価格範囲検索（JPQL）
    @Query("SELECT p FROM Product p WHERE p.price BETWEEN :min AND :max ORDER BY p.price")
    List<Product> findByPriceRange(@Param("min") BigDecimal min, @Param("max") BigDecimal max);
}
