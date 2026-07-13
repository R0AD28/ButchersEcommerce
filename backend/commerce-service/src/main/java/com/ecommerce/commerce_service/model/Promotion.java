package com.ecommerce.commerce_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "promotions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Promotion {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String code;
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;
    @Column(nullable = false)
    private Instant validFrom;
    @Column(nullable = false)
    private Instant validUntil;
    @Column(nullable = false)
    private boolean active;
}
