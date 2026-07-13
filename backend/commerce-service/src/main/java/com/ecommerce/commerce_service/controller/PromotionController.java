package com.ecommerce.commerce_service.controller;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.service.PromotionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/promotions")
public class PromotionController {
    private final PromotionService service;

    public PromotionController(PromotionService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody CreatePromotionRequest r, Authentication a,
            HttpServletRequest h) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.create(r, (Long) a.getPrincipal(), (String) a.getDetails(), h.getRemoteAddr()));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('MANAGE_PROMOTIONS')")
    public List<PromotionResponse> list() {
        return service.list();
    }
}
