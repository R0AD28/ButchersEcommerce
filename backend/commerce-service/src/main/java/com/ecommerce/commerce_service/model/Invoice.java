package com.ecommerce.commerce_service.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String invoiceNumber;
    @Column(nullable = false, unique = true)
    private Long orderId;
    @Column(nullable = false, length = 254)
    private String customerEmail;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;
    @Column(nullable = false)
    private Instant issuedAt;
}
