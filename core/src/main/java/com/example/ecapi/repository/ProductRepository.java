package com.example.ecapi.repository;

import com.example.ecapi.entity.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 商品リポジトリ
 *
 * <p>Spring Data JPA がメソッド名・@Query を元にクエリを自動生成する。
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // 低在庫アラート向け: 在庫が閾値以下の商品を在庫の少ない順に取得
    List<Product> findByDeletedFalseAndStockLessThanEqualOrderByStockAsc(int threshold);
}
