package com.example.ecapi.repository;

import com.example.ecapi.entity.ShippingAddress;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingAddressRepository extends JpaRepository<ShippingAddress, Long> {
    List<ShippingAddress> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Optional<ShippingAddress> findByIdAndCustomerId(Long id, Long customerId);

    boolean existsByCustomerId(Long customerId);

    List<ShippingAddress> findAllByCustomerIdAndIsDefaultTrue(Long customerId);
}
