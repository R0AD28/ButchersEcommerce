package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.dto.*;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.model.Order;
import com.ecommerce.commerce_service.repository.OrderRepository;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
 @Mock OrderRepository repository; @Mock CatalogClient catalog; @Mock PromotionService promotions;
 @Mock AuditOutboxService audit; @Mock IdempotencyService idempotency;
 OrderService service;
 @BeforeEach void setUp(){MockitoAnnotations.openMocks(this);service=new OrderService(repository,catalog,promotions,audit,idempotency);}
 @Test void createsOrderWithServerPrices(){
  when(catalog.getProduct(eq("CARNE-01"),anyString())).thenReturn(new CatalogProductResponse("CARNE-01","Carne",null,new BigDecimal("10.00"),true));
  when(promotions.discountFor(null,new BigDecimal("20.00"))).thenReturn(BigDecimal.ZERO);
  when(repository.saveAndFlush(any())).thenAnswer(i->{Order o=i.getArgument(0);o.setId(1L);return o;});
  OrderResponse result=service.create(new CreateOrderRequest(List.of(new OrderItemRequest("carne-01",2)),null),"key",7L,"client@test.com","jwt","127.0.0.1");
  assertEquals(new BigDecimal("20.00"),result.total()); assertEquals("CARNE-01",result.items().getFirst().sku());
  verify(idempotency).complete("key",7L); verify(audit).enqueue(eq(7L),eq("client@test.com"),eq("ORDER_CREATED"),any(),any(),any(),any(),any());
 }
 @Test void rejectsDuplicateSku(){
  when(catalog.getProduct(anyString(),anyString())).thenReturn(new CatalogProductResponse("A","A",null,BigDecimal.ONE,true));
  assertThrows(BusinessException.class,()->service.create(new CreateOrderRequest(List.of(new OrderItemRequest("A",1),new OrderItemRequest("a",2)),null),"k",1L,"a@b.com","jwt","ip"));
  verify(idempotency).release("k",1L);
 }
 @Test void rejectsUnavailableProduct(){
  when(catalog.getProduct(anyString(),anyString())).thenReturn(new CatalogProductResponse("A","A",null,BigDecimal.ONE,false));
  assertThrows(BusinessException.class,()->service.create(new CreateOrderRequest(List.of(new OrderItemRequest("A",1)),null),"k",1L,"a@b.com","jwt","ip"));
 }
 @Test void enforcesResourceOwnership(){
  Order o=Order.builder().id(1L).orderNumber("ORD-1").customerId(2L).build(); when(repository.findByOrderNumber("ORD-1")).thenReturn(Optional.of(o));
  assertThrows(BusinessException.class,()->service.findOwned("ORD-1",1L));
 }
}
