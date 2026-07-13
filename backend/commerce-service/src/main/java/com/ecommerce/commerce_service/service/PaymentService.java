package com.ecommerce.commerce_service.service;
import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.exception.*;
import com.ecommerce.commerce_service.model.*;
import com.ecommerce.commerce_service.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.UUID;

@Service
public class PaymentService {
 private final PaymentRepository payments; private final InvoiceRepository invoices; private final OrderService orders; private final AuditOutboxService audit; private final IdempotencyService idempotency;
 public PaymentService(PaymentRepository payments,InvoiceRepository invoices,OrderService orders,AuditOutboxService audit,IdempotencyService idempotency){this.payments=payments;this.invoices=invoices;this.orders=orders;this.audit=audit;this.idempotency=idempotency;}
 @Transactional
 public PaymentResponse process(ProcessPaymentRequest r,String key,Long userId,String email,String ip){
  idempotency.reserve(key,userId);
  try{
   Order order=orders.findOwned(r.orderNumber(),userId);
   if(payments.existsByOrderId(order.getId())) throw new BusinessException(HttpStatus.CONFLICT,ErrorCode.PAYMENT_ALREADY_PROCESSED,"La orden ya tiene un pago procesado");
   boolean approved=!r.paymentToken().toUpperCase().startsWith("REJECT");
   Payment p=payments.save(Payment.builder().paymentReference("PAY-"+UUID.randomUUID().toString().substring(0,10).toUpperCase()).orderId(order.getId()).amount(order.getTotal()).status(approved?PaymentStatus.APPROVED:PaymentStatus.REJECTED).paymentMethod(r.paymentMethod()).processedAt(Instant.now()).build());
   if(approved){orders.markPaid(order); invoices.save(Invoice.builder().invoiceNumber("FAC-"+UUID.randomUUID().toString().substring(0,10).toUpperCase()).orderId(order.getId()).customerEmail(order.getCustomerEmail()).subtotal(order.getSubtotal()).discount(order.getDiscount()).total(order.getTotal()).issuedAt(Instant.now()).build());}
   audit.enqueue(userId,email,approved?"PAYMENT_APPROVED":"PAYMENT_REJECTED","PAYMENT",p.getPaymentReference(),approved?"SUCCESS":"DENIED",ip,"order="+order.getOrderNumber()); idempotency.complete(key,userId);
   return new PaymentResponse(p.getPaymentReference(),order.getOrderNumber(),p.getAmount(),p.getStatus(),p.getPaymentMethod(),p.getProcessedAt());
  }catch(RuntimeException ex){idempotency.release(key,userId);throw ex;}
 }
 @Transactional(readOnly=true)
 public InvoiceResponse invoice(String orderNumber,Long userId){Order o=orders.findOwned(orderNumber,userId);Invoice i=invoices.findByOrderId(o.getId()).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,ErrorCode.INVOICE_NOT_FOUND,"La factura aún no existe"));return new InvoiceResponse(i.getInvoiceNumber(),o.getOrderNumber(),i.getCustomerEmail(),i.getSubtotal(),i.getDiscount(),i.getTotal(),i.getIssuedAt());}
}
