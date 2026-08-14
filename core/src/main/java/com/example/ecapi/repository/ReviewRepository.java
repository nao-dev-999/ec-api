package com.example.ecapi.repository;

import com.example.ecapi.entity.Review;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 商品レビューリポジトリ */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Page<Review> findAllByProductId(Long productId, Pageable pageable);

    Optional<Review> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerIdAndProductId(Long customerId, Long productId);

    long countByProductId(Long productId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.productId = :productId")
    Optional<Double> findAverageRatingByProductId(@Param("productId") Long productId);
}
