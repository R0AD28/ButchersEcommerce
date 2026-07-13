package com.ecommerce.commerce_service.repository;

import com.ecommerce.commerce_service.model.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {
    Optional<Promotion> findByCodeIgnoreCase(String code);
}
