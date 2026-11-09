CREATE TABLE products (
    id              UUID           PRIMARY KEY,
    sku             VARCHAR(64)    NOT NULL UNIQUE,
    name            VARCHAR(255)   NOT NULL,
    price           NUMERIC(12, 2) NOT NULL,
    stock_quantity  INTEGER        NOT NULL CHECK (stock_quantity >= 0),
    created_at      TIMESTAMPTZ    NOT NULL
);
