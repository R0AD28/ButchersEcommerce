package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.exception.*;
import com.ecommerce.commerce_service.model.Promotion;
import com.ecommerce.commerce_service.repository.PromotionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Service
public class PromotionService {
  private final PromotionRepository repository;
  private final AuditOutboxService audit;

  public PromotionService(PromotionRepository repository, AuditOutboxService audit) {
    this.repository = repository;
    this.audit = audit;
  }

  @Transactional
  public PromotionResponse create(CreatePromotionRequest r, Long userId, String email, String ip) {
    if (!r.validUntil().isAfter(r.validFrom()))
      throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PROMOTION_DATES,
          "validUntil debe ser posterior a validFrom");
    String code = r.code().trim().toUpperCase();
    if (repository.findByCodeIgnoreCase(code).isPresent())
      throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PROMOTION_ALREADY_EXISTS, "La promoción ya existe");
    Promotion p = repository.save(Promotion.builder().code(code).discountPercent(r.discountPercent())
        .validFrom(r.validFrom()).validUntil(r.validUntil()).active(r.active()).build());
    audit.enqueue(userId, email, "PROMOTION_CREATED", "PROMOTION", code, "SUCCESS", ip, "Promoción creada");
    return toResponse(p);
  }

  @Transactional(readOnly = true)
  public List<PromotionResponse> list() {
    return repository.findAll().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public BigDecimal discountFor(String code, BigDecimal subtotal) {
    if (code == null || code.isBlank())
      return BigDecimal.ZERO;
    Promotion p = repository.findByCodeIgnoreCase(code.trim()).orElseThrow(
        () -> new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PROMOTION, "Promoción inexistente"));
    Instant now = Instant.now();
    if (!p.isActive() || now.isBefore(p.getValidFrom()) || now.isAfter(p.getValidUntil()))
      throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.INVALID_PROMOTION,
          "Promoción inactiva o fuera de vigencia");
    return subtotal.multiply(p.getDiscountPercent()).divide(new BigDecimal("100.00")).setScale(2,
        java.math.RoundingMode.HALF_UP);
  }

  private PromotionResponse toResponse(Promotion p) {
    return new PromotionResponse(p.getCode(), p.getDiscountPercent(), p.getValidFrom(), p.getValidUntil(),
        p.isActive());
  }
}
