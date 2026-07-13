# Endpoints de catalog-service

Todos los endpoints de productos requieren `Authorization: Bearer <JWT>`.

- `GET /products?includeInactive=false`
- `GET /products/detail?sku=CARNE-001`
- `POST /products`
- `PUT /products`
- `PATCH /products/status`

No se utilizan IDs internos en las URLs. El identificador de negocio es `sku`.
