package com.example.ecapi.repository;

import com.example.ecapi.entity.Payment;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 決済リポジトリ */
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByCustomerOrderId(Long customerOrderId);
}
