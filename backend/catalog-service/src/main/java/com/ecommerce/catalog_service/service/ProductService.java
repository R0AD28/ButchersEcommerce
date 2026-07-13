package com.ecommerce.catalog_service.service;

import com.ecommerce.catalog_service.dto.*;
import com.ecommerce.catalog_service.exception.*;
import com.ecommerce.catalog_service.model.Product;
import com.ecommerce.catalog_service.repository.ProductRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Service
public class ProductService {

    private final ProductRepository repository;
    private final AuditOutboxService audit;
    private final IdempotencyService idempotency;

    public ProductService(ProductRepository repository, AuditOutboxService audit, IdempotencyService idempotency) {
        this.repository = repository;
        this.audit = audit;
        this.idempotency = idempotency;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findPublic(int page, int size) {
        Page<ProductResponse> result = repository.findAllByActiveTrue(pageable(page, size)).map(this::toPublic);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public ProductResponse findPublicBySku(String sku) {
        Product p = find(sku);
        if (!p.isActive()) {
            throw notFound();
        
        }return toPublic(p);
    }

    @Transactional(readOnly = true)
    public PageResponse<ManagementProductResponse> findForManagement(boolean includeInactive, int page, int size) {
        Page<Product> products = includeInactive ? repository.findAll(pageable(page, size)) : repository.findAllByActiveTrue(pageable(page, size));
        return PageResponse.from(products.map(this::toManagement));
    }

    @Transactional
    public ManagementProductResponse create(CreateProductRequest request, Long userId, String email, String ip) {
        String sku = normalizeSku(request.sku());
        if (repository.existsBySkuIgnoreCase(sku)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.PRODUCT_SKU_ALREADY_EXISTS, "Ya existe un producto con ese SKU");
        
        }Instant now = Instant.now();
        Product p = Product.builder().version(0L).sku(sku).name(request.name().trim()).description(normalizeDescription(request.description())).price(request.price()).stock(request.stock()).active(true).createdAt(now).updatedAt(now).createdBy(userId).updatedBy(userId).build();
        Product saved = repository.saveAndFlush(p);
        audit.enqueue(userId, email, "PRODUCT_CREATED", "PRODUCT", saved.getSku(), "SUCCESS", ip, "Producto creado");
        return toManagement(saved);
    }

    @Transactional
    public ManagementProductResponse update(UpdateProductRequest request, Long userId, String email, String ip) {
        Product p = find(request.sku());
        checkVersion(p, request.expectedVersion());
        p.setName(request.name().trim());
        p.setDescription(normalizeDescription(request.description()));
        p.setPrice(request.price());
        touch(p, userId);
        Product saved = repository.saveAndFlush(p);
        audit.enqueue(userId, email, "PRODUCT_UPDATED", "PRODUCT", saved.getSku(), "SUCCESS", ip, "Datos comerciales actualizados");
        return toManagement(saved);
    }

    @Transactional
    public ManagementProductResponse adjustInventory(InventoryAdjustmentRequest request, String key, Long userId, String email, String ip) {
        idempotency.reserve(key, userId);
        try {
            Product p = find(request.sku());
            checkVersion(p, request.expectedVersion());
            int current = p.getStock();
            int next = switch (request.operation()) {
                case INCREASE ->
                    current + request.quantity();
                case DECREASE ->
                    current - request.quantity();
                case SET ->
                    request.quantity();
            };
            if (next < 0) {
                throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.INSUFFICIENT_STOCK, "Stock insuficiente para completar la operación");
            
            }p.setStock(next);
            touch(p, userId);
            Product saved = repository.saveAndFlush(p);
            audit.enqueue(userId, email, "STOCK_" + request.operation().name(), "PRODUCT", saved.getSku(), "SUCCESS", ip, "Stock anterior=" + current + ", nuevo=" + next + ", motivo=" + request.reason().trim());
            idempotency.complete(key, userId);
            return toManagement(saved);
        } catch (RuntimeException ex) {
            idempotency.release(key, userId);
            throw ex;
        }
    }

    @Transactional
    public ManagementProductResponse changeStatus(ProductStatusRequest request, Long userId, String email, String ip) {
        Product p = find(request.sku());
        checkVersion(p, request.expectedVersion());
        if (p.isActive() == request.active()) {
            throw new BusinessException(HttpStatus.CONFLICT, request.active() ? ErrorCode.PRODUCT_ALREADY_ACTIVE : ErrorCode.PRODUCT_ALREADY_INACTIVE, request.active() ? "El producto ya está activo" : "El producto ya está inactivo");
        
        }p.setActive(request.active());
        touch(p, userId);
        Product saved = repository.saveAndFlush(p);
        audit.enqueue(userId, email, saved.isActive() ? "PRODUCT_ACTIVATED" : "PRODUCT_DEACTIVATED", "PRODUCT", saved.getSku(), "SUCCESS", ip, "Estado actualizado");
        return toManagement(saved);
    }

    private Pageable pageable(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "page debe ser >= 0 y size entre 1 y 100");
        
        }return PageRequest.of(page, size, Sort.by("name").ascending());
    }

    private Product find(String sku) {
        return repository.findBySkuIgnoreCase(normalizeSku(sku)).orElseThrow(this::notFound);
    }

    private BusinessException notFound() {
        return new BusinessException(HttpStatus.NOT_FOUND, ErrorCode.PRODUCT_NOT_FOUND, "No existe un producto con ese SKU");
    }

    private void checkVersion(Product p, Long expected) {
        if (!p.getVersion().equals(expected)) {
            throw new BusinessException(HttpStatus.CONFLICT, ErrorCode.CONCURRENT_MODIFICATION, "El producto cambió desde la última consulta");
    
        }}

    private void touch(Product p, Long userId) {
        p.setUpdatedAt(Instant.now());
        p.setUpdatedBy(userId);
    }

    private ProductResponse toPublic(Product p) {
        return new ProductResponse(p.getSku(), p.getName(), p.getDescription(), p.getPrice(), p.getStock() > 0);
    }

    private ManagementProductResponse toManagement(Product p) {
        return new ManagementProductResponse(p.getSku(), p.getName(), p.getDescription(), p.getPrice(), p.getStock(), p.isActive(), p.getVersion(), p.getCreatedAt(), p.getUpdatedAt());
    }

    private String normalizeSku(String sku) {
        return sku.trim().toUpperCase();
    }

    private String normalizeDescription(String d) {
        return d == null || d.isBlank() ? null : d.trim();
    }
}
