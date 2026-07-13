package com.ecommerce.commerce_service.controller;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.service.OrderService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/orders")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CREATE_ORDER')")
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest request,
            @RequestHeader("Idempotency-Key") String key, @RequestHeader("Authorization") String auth, Authentication a,
            HttpServletRequest http) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, key, (Long) a.getPrincipal(),
                (String) a.getDetails(), auth.substring(7), http.getRemoteAddr()));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAuthority('VIEW_OWN_ORDERS')")
    public List<OrderResponse> mine(Authentication a) {
        return service.mine((Long) a.getPrincipal());
    }

    @GetMapping("/{number}")
    @PreAuthorize("hasAuthority('VIEW_OWN_ORDERS')")
    public OrderResponse one(@PathVariable String number, Authentication a) {
        return service.getOwned(number, (Long) a.getPrincipal());
    }
}
