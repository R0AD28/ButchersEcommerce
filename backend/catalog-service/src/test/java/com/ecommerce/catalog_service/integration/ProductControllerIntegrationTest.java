package com.ecommerce.catalog_service.integration;

import com.ecommerce.catalog_service.dto.AuthorizationContext;
import com.ecommerce.catalog_service.model.Product;
import com.ecommerce.catalog_service.repository.AuditOutboxRepository;
import com.ecommerce.catalog_service.repository.ProductRepository;
import com.ecommerce.catalog_service.service.AuditClient;
import com.ecommerce.catalog_service.service.AuthorizationClient;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductControllerIntegrationTest {

    private static final Long USER_ID = 77L;
    private static final String EMAIL = "seller@test.local";

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("catalog_integration_test")
                    .withUsername("catalog_test")
                    .withPassword("catalog_test_password");

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("security.services.audit.outbox-delay-ms", () -> "3600000");
    }

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    AuditOutboxRepository auditOutboxRepository;

    @MockBean
    AuthorizationClient authorizationClient;

    @MockBean
    AuditClient auditClient;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        auditOutboxRepository.deleteAll();
        reset(authorizationClient, auditClient);
        token = createToken(USER_ID, EMAIL);
    }

    @Test
    void requestWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void userWithViewPermissionCanListOnlyActiveProducts() throws Exception {
        authorize("VIEW_PRODUCTS");
        saveProduct("ACTIVE-1", true, 10);
        saveProduct("INACTIVE-1", false, 5);

        mockMvc.perform(get("/products")
                        .header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(header().exists("X-Correlation-Id"))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].sku").value("ACTIVE-1"));
    }

    @Test
    void authenticatedUserWithoutPermissionReturns403() throws Exception {
        authorize();

        mockMvc.perform(get("/products")
                        .header("Authorization", bearer()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createProductPersistsProductAndAuditOutboxEvent() throws Exception {
        authorize("CREATE_PRODUCT");

        mockMvc.perform(post("/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": " meat-001 ",
                                  "name": "Lomo fino",
                                  "description": "Corte premium",
                                  "price": 12.50,
                                  "stock": 20
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/products/detail?sku=MEAT-001"))
                .andExpect(jsonPath("$.sku").value("MEAT-001"))
                .andExpect(jsonPath("$.stock").value(20))
                .andExpect(jsonPath("$.active").value(true));

        Product saved = productRepository.findBySkuIgnoreCase("MEAT-001").orElseThrow();
        assertThat(saved.getCreatedBy()).isEqualTo(USER_ID);
        assertThat(saved.getUpdatedBy()).isEqualTo(USER_ID);

        assertThat(auditOutboxRepository.findAll())
                .singleElement()
                .satisfies(event -> {
                    assertThat(event.getAction()).isEqualTo("PRODUCT_CREATED");
                    assertThat(event.getResourceId()).isEqualTo("MEAT-001");
                    assertThat(event.getUserEmail()).isEqualTo(EMAIL);
                });
    }

    @Test
    void duplicateSkuReturns409AndDoesNotCreateSecondProduct() throws Exception {
        authorize("CREATE_PRODUCT");
        saveProduct("MEAT-001", true, 10);

        mockMvc.perform(post("/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "meat-001",
                                  "name": "Duplicado",
                                  "price": 8.00,
                                  "stock": 3
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PRODUCT_SKU_ALREADY_EXISTS"));

        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidProductPayloadReturns400WithoutPersistingData() throws Exception {
        authorize("CREATE_PRODUCT");

        mockMvc.perform(post("/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU INVÁLIDO!",
                                  "name": "",
                                  "price": -1,
                                  "stock": -4
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        assertThat(productRepository.count()).isZero();
        assertThat(auditOutboxRepository.count()).isZero();
    }

    @Test
    void updateWithStaleVersionReturns409() throws Exception {
        authorize("UPDATE_PRODUCT");
        Product product = saveProduct("MEAT-002", true, 10);

        mockMvc.perform(put("/products")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "MEAT-002",
                                  "expectedVersion": 99,
                                  "name": "Nombre actualizado",
                                  "description": "Cambio",
                                  "price": 15.00
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONCURRENT_MODIFICATION"));

        Product unchanged = productRepository.findById(product.getId()).orElseThrow();
        assertThat(unchanged.getName()).isEqualTo("Producto MEAT-002");
    }

    @Test
    void inventoryAdjustmentIsIdempotentAndCannotBeRepeated() throws Exception {
        authorize("UPDATE_PRODUCT");
        Product product = saveProduct("MEAT-003", true, 10);

        String body = """
                {
                  "sku": "MEAT-003",
                  "expectedVersion": %d,
                  "operation": "DECREASE",
                  "quantity": 3,
                  "reason": "Venta confirmada"
                }
                """.formatted(product.getVersion());

        mockMvc.perform(patch("/products/inventory")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", "order-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(7));

        mockMvc.perform(patch("/products/inventory")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", "order-1001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_REQUEST"));

        assertThat(productRepository.findBySkuIgnoreCase("MEAT-003").orElseThrow().getStock())
                .isEqualTo(7);
    }

    @Test
    void inventoryCannotBecomeNegative() throws Exception {
        authorize("UPDATE_PRODUCT");
        Product product = saveProduct("MEAT-004", true, 2);

        mockMvc.perform(patch("/products/inventory")
                        .header("Authorization", bearer())
                        .header("Idempotency-Key", "order-1002")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "MEAT-004",
                                  "expectedVersion": %d,
                                  "operation": "DECREASE",
                                  "quantity": 5,
                                  "reason": "Prueba de stock"
                                }
                                """.formatted(product.getVersion())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INSUFFICIENT_STOCK"));

        assertThat(productRepository.findBySkuIgnoreCase("MEAT-004").orElseThrow().getStock())
                .isEqualTo(2);
    }

    @Test
    void onlyDeletePermissionCanDeactivateProduct() throws Exception {
        Product product = saveProduct("MEAT-005", true, 4);
        authorize("UPDATE_PRODUCT");

        String body = """
                {
                  "sku": "MEAT-005",
                  "expectedVersion": %d,
                  "active": false
                }
                """.formatted(product.getVersion());

        mockMvc.perform(patch("/products/status")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());

        authorize("DELETE_PRODUCT");
        mockMvc.perform(patch("/products/status")
                        .header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        assertThat(productRepository.findBySkuIgnoreCase("MEAT-005").orElseThrow().isActive())
                .isFalse();
    }

    private void authorize(String... permissions) {
        when(authorizationClient.getAuthorizationContext(anyString()))
                .thenReturn(new AuthorizationContext(
                        USER_ID,
                        EMAIL,
                        true,
                        false,
                        Set.of("VENDEDOR"),
                        Set.of(permissions)
                ));
    }

    private Product saveProduct(String sku, boolean active, int stock) {
        Instant now = Instant.now();
        return productRepository.saveAndFlush(Product.builder()
                .sku(sku)
                .name("Producto " + sku)
                .description("Descripción")
                .price(new BigDecimal("10.00"))
                .stock(stock)
                .active(active)
                .createdAt(now)
                .updatedAt(now)
                .createdBy(USER_ID)
                .updatedBy(USER_ID)
                .build());
    }

    private String bearer() {
        return "Bearer " + token;
    }

    private static String createToken(Long userId, String email) throws Exception {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .issuer("auth-service-test")
                .audience().add("butchers-ecommerce-test").and()
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(600)))
                .signWith(loadPrivateKey(), Jwts.SIG.RS256)
                .compact();
    }

    private static PrivateKey loadPrivateKey() throws Exception {
        try (var input = ProductControllerIntegrationTest.class
                .getResourceAsStream("/test-keys/private-key.pem")) {
            if (input == null) {
                throw new IllegalStateException("No se encontró la clave privada de pruebas");
            }
            String pem = new String(input.readAllBytes(), StandardCharsets.UTF_8)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] encoded = Base64.getDecoder().decode(pem);
            return KeyFactory.getInstance("RSA")
                    .generatePrivate(new PKCS8EncodedKeySpec(encoded));
        }
    }
}
