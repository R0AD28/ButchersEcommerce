package com.ecommerce.commerce_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 60)
    private String paymentReference;
    @Column(nullable = false, unique = true)
    private Long orderId;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;
    @Column(nullable = false, length = 30)
    private String paymentMethod;
    @Column(nullable = false)
    private Instant processedAt;
}
