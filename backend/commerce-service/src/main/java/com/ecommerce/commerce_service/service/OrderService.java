package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.exception.*;
import com.ecommerce.commerce_service.model.*;
import com.ecommerce.commerce_service.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository repository;
    private final CatalogClient catalog;
    private final PromotionService promotions;
    private final AuditOutboxService audit;
    private final IdempotencyService idempotency;

    public OrderService(OrderRepository repository, CatalogClient catalog, PromotionService promotions,
            AuditOutboxService audit, IdempotencyService idempotency) {
        this.repository = repository;
        this.catalog = catalog;
        this.promotions = promotions;
        this.audit = audit;
        this.idempotency = idempotency;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest r, String key, Long userId, String email, String token, String ip) {
        idempotency.reserve(key, userId);
        try {
            Set<String> seen = new HashSet<>();
            List<OrderItem> items = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;
            for (OrderItemRequest item : r.items()) {
                String sku = item.sku().trim().toUpperCase();
                if (!seen.add(sku))
                    throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.DUPLICATE_ORDER_ITEM,
                            "No se puede repetir el mismo SKU");
                CatalogProductResponse product = catalog.getProduct(sku, token);
                if (product == null || !product.available())
                    throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_UNAVAILABLE,
                            "Producto no disponible: " + sku);
                BigDecimal line = product.price().multiply(BigDecimal.valueOf(item.quantity())).setScale(2);
                subtotal = subtotal.add(line);
                items.add(OrderItem.builder().productSku(sku).productName(product.name()).quantity(item.quantity())
                        .unitPrice(product.price()).lineTotal(line).build());
            }
            BigDecimal discount = promotions.discountFor(r.promotionCode(), subtotal);
            Instant now = Instant.now();
            Order order = Order.builder().version(0L)
                    .orderNumber("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()).customerId(userId)
                    .customerEmail(email).status(OrderStatus.PENDING_PAYMENT).subtotal(subtotal).discount(discount)
                    .total(subtotal.subtract(discount)).promotionCode(normalize(r.promotionCode())).createdAt(now)
                    .updatedAt(now).items(items).build();
            items.forEach(i -> i.setOrder(order));
            Order saved = repository.saveAndFlush(order);
            audit.enqueue(userId, email, "ORDER_CREATED", "ORDER", saved.getOrderNumber(), "SUCCESS", ip,
                    "Orden creada; total=" + saved.getTotal());
            idempotency.complete(key, userId);
            return toResponse(saved);
        } catch (RuntimeException ex) {
            idempotency.release(key, userId);
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> mine(Long userId) {
        return repository.findAllByCustomerIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOwned(String number, Long userId) {
        return toResponse(findOwned(number, userId));
    }

    @Transactional(readOnly = true)
    public Order findOwned(String number, Long userId) {
        Order o = find(number);
        if (!o.getCustomerId().equals(userId))
            throw new BusinessException(HttpStatus.FORBIDDEN, ErrorCode.RESOURCE_NOT_OWNED,
                    "La orden pertenece a otro cliente");
        return o;
    }

    @Transactional(readOnly = true)
    public Order find(String number) {
        return repository.findByOrderNumber(number.trim().toUpperCase()).orElseThrow(
                () -> new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.ORDER_NOT_FOUND, "Orden no encontrada"));
    }

    @Transactional
    public void markPaid(Order o) {
        if (o.getStatus() != OrderStatus.PENDING_PAYMENT)
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INVALID_ORDER_STATE,
                    "La orden no está pendiente de pago");
        o.setStatus(OrderStatus.PAID);
        o.setUpdatedAt(Instant.now());
        repository.saveAndFlush(o);
    }

    private String normalize(String s) {
        return s == null || s.isBlank() ? null : s.trim().toUpperCase();
    }

    public OrderResponse toResponse(Order o) {
        return new OrderResponse(o.getOrderNumber(), o.getCustomerEmail(), o.getStatus(), o.getSubtotal(),
                o.getDiscount(), o.getTotal(), o.getPromotionCode(), o.getVersion(), o.getCreatedAt(),
                o.getItems().stream().map(i -> new OrderItemResponse(i.getProductSku(), i.getProductName(),
                        i.getQuantity(), i.getUnitPrice(), i.getLineTotal())).toList());
    }
}
