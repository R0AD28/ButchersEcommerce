package com.ecommerce.commerce_service.service;
import com.ecommerce.commerce_service.dto.CreatePromotionRequest;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.model.Promotion;
import com.ecommerce.commerce_service.repository.PromotionRepository;
import org.junit.jupiter.api.*; import org.mockito.*;
import java.math.BigDecimal; import java.time.Instant; import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*; import static org.mockito.Mockito.*;
class PromotionServiceTest {
 @Mock PromotionRepository repository; @Mock AuditOutboxService audit; PromotionService service;
 @BeforeEach void setup(){MockitoAnnotations.openMocks(this);service=new PromotionService(repository,audit);}
 @Test void calculatesValidDiscount(){Promotion p=Promotion.builder().code("TEN").discountPercent(new BigDecimal("10")).validFrom(Instant.now().minusSeconds(60)).validUntil(Instant.now().plusSeconds(60)).active(true).build();when(repository.findByCodeIgnoreCase("TEN")).thenReturn(Optional.of(p));assertEquals(new BigDecimal("10.00"),service.discountFor("TEN",new BigDecimal("100.00")));}
 @Test void rejectsExpiredPromotion(){Promotion p=Promotion.builder().code("OLD").discountPercent(BigDecimal.TEN).validFrom(Instant.now().minusSeconds(120)).validUntil(Instant.now().minusSeconds(60)).active(true).build();when(repository.findByCodeIgnoreCase("OLD")).thenReturn(Optional.of(p));assertThrows(BusinessException.class,()->service.discountFor("OLD",BigDecimal.TEN));}
 @Test void rejectsInvalidDateRange(){Instant now=Instant.now();assertThrows(BusinessException.class,()->service.create(new CreatePromotionRequest("ABC",BigDecimal.TEN,now,now.minusSeconds(1),true),1L,"a@b.com","ip"));}
}
