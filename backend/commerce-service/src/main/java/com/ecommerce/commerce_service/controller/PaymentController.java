package com.ecommerce.commerce_service.controller;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {
    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/payments")
    @PreAuthorize("hasAuthority('PROCESS_PAYMENT')")
    public PaymentResponse pay(@Valid @RequestBody ProcessPaymentRequest r,
            @RequestHeader("Idempotency-Key") String key, Authentication a, HttpServletRequest h) {
        return service.process(r, key, (Long) a.getPrincipal(), (String) a.getDetails(), h.getRemoteAddr());
    }

    @GetMapping("/invoices/{orderNumber}")
    @PreAuthorize("hasAuthority('VIEW_OWN_INVOICES')")
    public InvoiceResponse invoice(@PathVariable String orderNumber, Authentication a) {
        return service.invoice(orderNumber, (Long) a.getPrincipal());
    }
}
