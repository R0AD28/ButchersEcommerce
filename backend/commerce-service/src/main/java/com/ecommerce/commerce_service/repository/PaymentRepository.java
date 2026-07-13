package com.ecommerce.commerce_service.repository;

import com.ecommerce.commerce_service.model.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
