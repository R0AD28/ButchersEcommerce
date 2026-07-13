package com.ecommerce.commerce_service.repository;

import com.ecommerce.commerce_service.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);

    List<Order> findAllByCustomerIdOrderByCreatedAtDesc(Long customerId);
}
