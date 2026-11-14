package com.example.ecapi.repository;

import com.example.ecapi.entity.Product;
import jakarta.persistence.EntityManager;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 商品のキーワード検索リポジトリ
 *
 * <p>並び順を画面から切り替えられるようにするため、JPQL を組み立てて検索する。
 */
@Repository
@RequiredArgsConstructor
public class ProductSearchRepository {

    private final EntityManager entityManager;

    /** 商品名で部分一致検索し、{@code sortBy} で指定した項目の昇順で返します。 */
    public List<Product> searchByName(String keyword, String sortBy, int limit) {
        String jpql =
                "SELECT p FROM Product p WHERE p.deleted = false AND lower(p.name) LIKE :keyword"
                        + " ORDER BY p."
                        + sortBy;
        return entityManager
                .createQuery(jpql, Product.class)
                .setParameter("keyword", "%" + keyword.toLowerCase() + "%")
                .setMaxResults(limit)
                .getResultList();
    }
}
