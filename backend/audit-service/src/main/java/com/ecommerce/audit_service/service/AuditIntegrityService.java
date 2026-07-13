package com.ecommerce.audit_service.service;

import com.ecommerce.audit_service.dto.IntegrityVerificationResponse;
import com.ecommerce.audit_service.model.AuditEvent;
import com.ecommerce.audit_service.repository.AuditEventRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/**
 * Servicio encargado de calcular y verificar la integridad
 * criptográfica de los eventos de auditoría.
 *
 * Cada evento almacena:
 * - El hash del evento anterior.
 * - Su propio hash HMAC-SHA256.
 *
 * De esta manera se forma una cadena de auditoría que permite
 * detectar modificaciones realizadas directamente en la base de datos.
 */
@Service
public class AuditIntegrityService {

    /**
     * Algoritmo utilizado para generar el código HMAC.
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    /**
     * Valor utilizado como hash anterior del primer evento.
     *
     * Está compuesto por 64 ceros porque un hash SHA-256
     * representado en hexadecimal tiene 64 caracteres.
     */
    private static final String GENESIS_HASH = "0".repeat(64);

    private final AuditEventRepository repository;

    /**
     * Secreto utilizado para firmar los registros.
     *
     * Este secreto debe configurarse mediante:
     *
     * AUDIT_INTEGRITY_SECRET
     *
     * No debe almacenarse en el repositorio.
     */
    private final String integritySecret;

    /**
     * Reloj utilizado para obtener la fecha actual.
     *
     * Utilizar Clock facilita las pruebas porque permite
     * proporcionar una fecha controlada.
     */
    private final Clock clock;

    /**
     * Constructor utilizado normalmente por Spring.
     */
    public AuditIntegrityService(
            AuditEventRepository repository,

            @Value("${security.audit.integrity-secret}") String integritySecret) {
        this(
                repository,
                integritySecret,
                Clock.systemUTC());
    }

    /**
     * Constructor utilizado principalmente en pruebas.
     *
     * Permite proporcionar un reloj personalizado.
     */
    AuditIntegrityService(
            AuditEventRepository repository,
            String integritySecret,
            Clock clock) {
        validateSecret(integritySecret);
        this.repository = repository;
        this.integritySecret = integritySecret;
        this.clock = clock;
    }

    /**
     * Valida el secreto después de que Spring crea el servicio.
     *
     * El servicio no debe iniciar si:
     * - El secreto no está configurado.
     * - El secreto está vacío.
     * - El secreto tiene menos de 32 caracteres.
     */
    private static void validateSecret(String integritySecret) {

        if (integritySecret == null
                || integritySecret.isBlank()) {

            throw new IllegalStateException(
                    "AUDIT_INTEGRITY_SECRET no está configurado");
        }

        if (integritySecret.length() < 32) {

            throw new IllegalStateException(
                    "AUDIT_INTEGRITY_SECRET debe tener al menos 32 caracteres");
        }
    }

    /**
     * Obtiene el hash del último evento almacenado.
     *
     * Este valor se utilizará como previousHash del próximo evento.
     *
     * Si todavía no existe ningún evento, devuelve el hash génesis.
     */
    public String currentPreviousHash() {

        return repository
                .findFirstByOrderByRecordedAtDescIdDesc()
                .map(AuditEvent::getEventHash)
                .orElse(GENESIS_HASH);
    }

    /**
     * Calcula el HMAC-SHA256 de un evento.
     *
     * Todos los campos importantes se combinan en un único texto
     * con un formato estable. Después se firma el contenido con
     * el secreto de integridad.
     *
     * @param event evento del cual se calculará el hash.
     * @return hash HMAC representado en hexadecimal.
     */
    public String calculateHash(
            AuditEvent event) {

        /*
         * Construye una representación canónica del evento.
         *
         * El orden de los campos es importante y debe mantenerse
         * igual durante la creación y durante la verificación.
         */
        String canonical = String.join(
                "|",
                safe(event.getPreviousHash()),
                safe(event.getUserId()),
                safe(event.getUserEmail()),
                safe(event.getSourceService()),
                safe(event.getAction()),
                safe(event.getResource()),
                safe(event.getResourceId()),
                safe(event.getResult()),
                safe(event.getIpAddress()),
                safe(event.getCorrelationId()),
                safe(event.getDetail()),
                safe(event.getOccurredAt()),
                safe(event.getRecordedAt()),
                Boolean.toString(event.isCritical()));

        try {

            /*
             * Crea una instancia del algoritmo HMAC-SHA256.
             */
            Mac mac = Mac.getInstance(
                    HMAC_ALGORITHM);

            /*
             * Convierte el secreto configurado en una clave
             * compatible con HMAC-SHA256.
             */
            SecretKeySpec secretKey = new SecretKeySpec(
                    integritySecret.getBytes(
                            StandardCharsets.UTF_8),
                    HMAC_ALGORITHM);

            /*
             * Inicializa el algoritmo con la clave secreta.
             */
            mac.init(secretKey);

            /*
             * Calcula el HMAC del contenido canónico.
             */
            byte[] hashBytes = mac.doFinal(
                    canonical.getBytes(
                            StandardCharsets.UTF_8));

            /*
             * Convierte los bytes del hash a una cadena hexadecimal.
             */
            return HexFormat
                    .of()
                    .formatHex(hashBytes);

        } catch (
                NoSuchAlgorithmException
                | InvalidKeyException exception) {

            throw new IllegalStateException(
                    "No se pudo calcular el HMAC de auditoría",
                    exception);
        }
    }

    /**
     * Verifica la integridad completa de la cadena de auditoría.
     *
     * Para cada evento se comprueba:
     * 1. Que previousHash coincida con el hash del evento anterior.
     * 2. Que eventHash coincida con el HMAC recalculado.
     *
     * @return resultado de la verificación.
     */
    public IntegrityVerificationResponse verifyChain() {

        /*
         * Recupera los eventos en el mismo orden
         * en el que fueron almacenados.
         */
        List<AuditEvent> events = repository.findAllByOrderByRecordedAtAscIdAsc();

        String expectedPreviousHash = GENESIS_HASH;

        long checkedEvents = 0;

        for (AuditEvent event : events) {

            checkedEvents++;

            /*
             * Verifica que el evento apunte correctamente
             * al hash del evento anterior.
             */
            if (!Objects.equals(
                    expectedPreviousHash,
                    event.getPreviousHash())) {

                return invalid(
                        event,
                        checkedEvents,
                        "La referencia al hash anterior no coincide");
            }

            /*
             * Recalcula el hash del evento actual.
             */
            String expectedHash = calculateHash(event);

            /*
             * Compara el hash recalculado con el almacenado.
             */
            if (!Objects.equals(
                    expectedHash,
                    event.getEventHash())) {

                return invalid(
                        event,
                        checkedEvents,
                        "El HMAC del evento no coincide");
            }

            /*
             * El hash actual será el hash anterior
             * esperado para el siguiente evento.
             */
            expectedPreviousHash = event.getEventHash();
        }

        return new IntegrityVerificationResponse(
                true,
                checkedEvents,
                null,
                "Cadena de auditoría íntegra",
                clock.instant());
    }

    /**
     * Construye una respuesta cuando se detecta
     * una alteración en la cadena.
     */
    private IntegrityVerificationResponse invalid(
            AuditEvent event,
            long checkedEvents,
            String message) {

        return new IntegrityVerificationResponse(
                false,
                checkedEvents,
                event.getId(),
                message,
                clock.instant());
    }

    /**
     * Convierte valores nulos a cadenas vacías.
     *
     * Esto garantiza que el formato utilizado para calcular
     * el hash siempre sea consistente.
     */
    private String safe(
            Object value) {
        return value == null
                ? ""
                : value.toString();
    }
}