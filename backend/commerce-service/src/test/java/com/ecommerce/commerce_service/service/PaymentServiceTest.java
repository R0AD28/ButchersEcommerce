package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.dto.PaymentResponse;
import com.ecommerce.commerce_service.dto.ProcessPaymentRequest;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.model.Order;
import com.ecommerce.commerce_service.model.OrderStatus;
import com.ecommerce.commerce_service.model.PaymentStatus;
import com.ecommerce.commerce_service.repository.InvoiceRepository;
import com.ecommerce.commerce_service.repository.PaymentRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    @Mock
    private PaymentRepository payments;

    @Mock
    private InvoiceRepository invoices;

    @Mock
    private OrderService orders;

    @Mock
    private AuditOutboxService audit;

    @Mock
    private IdempotencyService idempotency;

    private PaymentService service;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);

        service = new PaymentService(
                payments,
                invoices,
                orders,
                audit,
                idempotency);
    }

    private Order order() {
        return Order.builder()
                .id(10L)
                .orderNumber("ORD-1")
                .customerId(1L)
                .customerEmail("a@b.com")
                .status(OrderStatus.PENDING_PAYMENT)
                .subtotal(new BigDecimal("20.00"))
                .discount(new BigDecimal("2.00"))
                .total(new BigDecimal("18.00"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void approvedPaymentMarksPaidAndCreatesInvoice() {
        Order order = order();

        when(orders.findOwned("ORD-1", 1L))
                .thenReturn(order);

        when(payments.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.process(
                new ProcessPaymentRequest(
                        "ORD-1",
                        "CARD",
                        "approved-token"),
                "k",
                1L,
                "a@b.com",
                "ip");

        assertEquals(
                PaymentStatus.APPROVED,
                response.status());

        verify(orders).markPaid(order);
        verify(invoices).save(any());
    }

    @Test
    void rejectedPaymentDoesNotCreateInvoice() {
        Order order = order();

        when(orders.findOwned("ORD-1", 1L))
                .thenReturn(order);

        when(payments.save(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = service.process(
                new ProcessPaymentRequest(
                        "ORD-1",
                        "CARD",
                        "REJECT-001"),
                "k",
                1L,
                "a@b.com",
                "ip");

        assertEquals(
                PaymentStatus.REJECTED,
                response.status());

        verify(orders, never()).markPaid(any());
        verify(invoices, never()).save(any());
    }

    @Test
    void preventsSecondPayment() {
        when(orders.findOwned("ORD-1", 1L))
                .thenReturn(order());

        when(payments.existsByOrderId(10L))
                .thenReturn(true);

        assertThrows(
                BusinessException.class,
                () -> service.process(
                        new ProcessPaymentRequest(
                                "ORD-1",
                                "CARD",
                                "approved-token"),
                        "k",
                        1L,
                        "a@b.com",
                        "ip"));

        verify(idempotency).release("k", 1L);
    }
}