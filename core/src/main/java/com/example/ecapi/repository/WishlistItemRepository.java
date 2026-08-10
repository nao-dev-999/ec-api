package com.example.ecapi.repository;

import com.example.ecapi.entity.WishlistItem;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Page<WishlistItem> findAllByCustomerId(Long customerId, Pageable pageable);

    Optional<WishlistItem> findByCustomerIdAndProductId(Long customerId, Long productId);

    void deleteByCustomerIdAndProductId(Long customerId, Long productId);
}
