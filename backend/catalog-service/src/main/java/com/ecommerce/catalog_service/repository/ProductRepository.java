package com.ecommerce.catalog_service.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.catalog_service.model.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCase(String sku);
    Page<Product> findAllByActiveTrue(Pageable pageable);
}
