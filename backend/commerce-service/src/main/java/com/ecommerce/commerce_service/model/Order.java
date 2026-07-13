package com.ecommerce.commerce_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    private Long version;
    @Column(nullable = false, unique = true, length = 40)
    private String orderNumber;
    @Column(nullable = false)
    private Long customerId;
    @Column(nullable = false, length = 254)
    private String customerEmail;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(length = 50)
    private String promotionCode;
    @Column(nullable = false)
    private Instant createdAt;
    @Column(nullable = false)
    private Instant updatedAt;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();
}
