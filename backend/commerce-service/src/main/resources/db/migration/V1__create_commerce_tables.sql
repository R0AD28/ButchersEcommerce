CREATE TABLE orders (
 id BIGSERIAL PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0,
 order_number VARCHAR(40) NOT NULL UNIQUE, customer_id BIGINT NOT NULL,
 customer_email VARCHAR(254) NOT NULL, status VARCHAR(30) NOT NULL,
 subtotal NUMERIC(12,2) NOT NULL, discount NUMERIC(12,2) NOT NULL DEFAULT 0,
 total NUMERIC(12,2) NOT NULL, promotion_code VARCHAR(50),
 created_at TIMESTAMPTZ NOT NULL, updated_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_order_amounts CHECK (subtotal >= 0 AND discount >= 0 AND total >= 0),
 CONSTRAINT chk_order_status CHECK (status IN ('PENDING_PAYMENT','PAID','CANCELLED'))
);
CREATE INDEX idx_orders_customer ON orders(customer_id, created_at DESC);
CREATE TABLE order_items (
 id BIGSERIAL PRIMARY KEY, order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
 product_sku VARCHAR(50) NOT NULL, product_name VARCHAR(150) NOT NULL,
 quantity INTEGER NOT NULL, unit_price NUMERIC(12,2) NOT NULL, line_total NUMERIC(12,2) NOT NULL,
 CONSTRAINT chk_order_item_values CHECK (quantity > 0 AND unit_price >= 0 AND line_total >= 0),
 UNIQUE(order_id, product_sku)
);
CREATE TABLE payments (
 id BIGSERIAL PRIMARY KEY, payment_reference VARCHAR(60) NOT NULL UNIQUE,
 order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id), amount NUMERIC(12,2) NOT NULL,
 status VARCHAR(20) NOT NULL, payment_method VARCHAR(30) NOT NULL, processed_at TIMESTAMPTZ NOT NULL,
 CONSTRAINT chk_payment_status CHECK (status IN ('APPROVED','REJECTED')),
 CONSTRAINT chk_payment_amount CHECK (amount >= 0)
);
CREATE TABLE invoices (
 id BIGSERIAL PRIMARY KEY, invoice_number VARCHAR(50) NOT NULL UNIQUE,
 order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id), customer_email VARCHAR(254) NOT NULL,
 subtotal NUMERIC(12,2) NOT NULL, discount NUMERIC(12,2) NOT NULL, total NUMERIC(12,2) NOT NULL,
 issued_at TIMESTAMPTZ NOT NULL
);
CREATE TABLE promotions (
 id BIGSERIAL PRIMARY KEY, code VARCHAR(50) NOT NULL UNIQUE,
 discount_percent NUMERIC(5,2) NOT NULL, valid_from TIMESTAMPTZ NOT NULL,
 valid_until TIMESTAMPTZ NOT NULL, active BOOLEAN NOT NULL,
 CONSTRAINT chk_promotion_discount CHECK (discount_percent > 0 AND discount_percent <= 100),
 CONSTRAINT chk_promotion_dates CHECK (valid_until > valid_from)
);
