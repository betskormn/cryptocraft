CREATE TABLE brands
(
    brand_id   SERIAL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products
(
    product_id        BIGSERIAL PRIMARY KEY,
    name              VARCHAR(255) NOT NULL,
    description       TEXT,
    image             VARCHAR(255),
    quantity_in_stock INT          NOT NULL,
    unit              SMALLINT     NOT NULL,
    brand_id          BIGINT       NOT NULL,
    created_at        TIMESTAMP,
    updated_at        TIMESTAMP,
    FOREIGN KEY (brand_id) REFERENCES brands (brand_id)
);

