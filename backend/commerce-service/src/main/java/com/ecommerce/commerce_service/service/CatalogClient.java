package com.ecommerce.commerce_service.service;

import com.ecommerce.commerce_service.dto.CatalogProductResponse;
import com.ecommerce.commerce_service.exception.BusinessException;
import com.ecommerce.commerce_service.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class CatalogClient {
    private final RestClient restClient;
    private final String baseUrl;

    public CatalogClient(RestClient.Builder builder, @Value("${clients.catalog.base-url}") String baseUrl) {
        this.restClient = builder.build();
        this.baseUrl = baseUrl;
    }

    public CatalogProductResponse getProduct(String sku, String bearerToken) {
        try {
            return restClient.get().uri(baseUrl + "/products/{sku}", sku)
                    .header("Authorization", "Bearer " + bearerToken)
                    .retrieve().body(CatalogProductResponse.class);
        } catch (RuntimeException ex) {
            throw new BusinessException(HttpStatus.BAD_GATEWAY, ErrorCode.CATALOG_UNAVAILABLE,
                    "No fue posible validar el producto en catalog-service");
        }
    }
}
