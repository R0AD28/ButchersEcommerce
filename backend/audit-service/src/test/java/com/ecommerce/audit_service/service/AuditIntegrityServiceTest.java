package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.IntegrityVerificationResponse;
import com.ecommerce.audit_service.model.AuditEvent;
import com.ecommerce.audit_service.repository.AuditEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditIntegrityServiceTest {

    private static final String SECRET =
            "test-integrity-secret-with-more-than-32-characters";
    private static final String GENESIS_HASH = "0".repeat(64);
    private static final Instant NOW = Instant.parse("2026-07-12T16:00:00Z");

    @Mock
    private AuditEventRepository repository;

    private AuditIntegrityService service;

    @BeforeEach
    void setUp() {
        service = new AuditIntegrityService(
                repository,
                SECRET,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void constructorRejectsShortSecret() {
        assertThrows(
                IllegalStateException.class,
                () -> new AuditIntegrityService(
                        repository,
                        "short",
                        Clock.fixed(NOW, ZoneOffset.UTC)
                )
        );
    }

    @Test
    void currentPreviousHashReturnsGenesisWhenRepositoryIsEmpty() {
        when(repository.findFirstByOrderByRecordedAtDescIdDesc())
                .thenReturn(Optional.empty());

        assertEquals(GENESIS_HASH, service.currentPreviousHash());
    }

    @Test
    void currentPreviousHashReturnsLastStoredHash() {
        AuditEvent lastEvent = AuditEvent.builder()
                .eventHash("a".repeat(64))
                .build();
        when(repository.findFirstByOrderByRecordedAtDescIdDesc())
                .thenReturn(Optional.of(lastEvent));

        assertEquals("a".repeat(64), service.currentPreviousHash());
    }

    @Test
    void calculateHashIsDeterministicAndHasSha256HexLength() {
        AuditEvent event = event(UUID.randomUUID(), GENESIS_HASH);

        String first = service.calculateHash(event);
        String second = service.calculateHash(event);

        assertEquals(first, second);
        assertEquals(64, first.length());
    }

    @Test
    void verifyChainReturnsValidForCorrectChain() {
        AuditEvent first = event(UUID.randomUUID(), GENESIS_HASH);
        first.setEventHash(service.calculateHash(first));

        AuditEvent second = event(UUID.randomUUID(), first.getEventHash());
        second.setCorrelationId("correlation-2");
        second.setEventHash(service.calculateHash(second));

        when(repository.findAllByOrderByRecordedAtAscIdAsc())
                .thenReturn(List.of(first, second));

        IntegrityVerificationResponse response = service.verifyChain();

        assertTrue(response.valid());
        assertEquals(2, response.checkedEvents());
        assertEquals(NOW, response.verifiedAt());
    }

    @Test
    void verifyChainDetectsModifiedEvent() {
        AuditEvent event = event(UUID.randomUUID(), GENESIS_HASH);
        event.setEventHash(service.calculateHash(event));
        event.setDetail("contenido alterado después de firmar");

        when(repository.findAllByOrderByRecordedAtAscIdAsc())
                .thenReturn(List.of(event));

        IntegrityVerificationResponse response = service.verifyChain();

        assertFalse(response.valid());
        assertEquals(event.getId(), response.firstInvalidEventId());
        assertEquals("El HMAC del evento no coincide", response.message());
    }

    private AuditEvent event(UUID id, String previousHash) {
        return AuditEvent.builder()
                .id(id)
                .userId("10")
                .userEmail("user@example.com")
                .sourceService("AUTH-SERVICE")
                .action("LOGIN_SUCCESS")
                .resource("USER")
                .resourceId("10")
                .result("SUCCESS")
                .ipAddress("10.0.0.1")
                .correlationId("correlation-1")
                .detail("Inicio de sesión")
                .occurredAt(Instant.parse("2026-07-12T15:59:00Z"))
                .recordedAt(NOW)
                .critical(false)
                .previousHash(previousHash)
                .build();
    }
}
