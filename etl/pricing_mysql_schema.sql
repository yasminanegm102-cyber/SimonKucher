-- MySQL schema for Pricing App ETL integration
-- Aligned with application schema (V1__init.sql)

-- Buildings table (must be created first due to FK dependencies)
CREATE TABLE IF NOT EXISTS buildings (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    type VARCHAR(64)
);

-- Products table
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(64) PRIMARY KEY,
    building_id VARCHAR(64),
    room_name VARCHAR(128),
    arrival_date DATE,
    no_of_beds INT,
    room_type VARCHAR(64),
    grade INT,
    private_pool BOOLEAN,
    CONSTRAINT fk_products_building FOREIGN KEY (building_id) REFERENCES buildings(id)
);

-- Bookings table
CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64),
    arrival_date DATE,
    nights INT,
    price_paid DOUBLE,
    CONSTRAINT fk_bookings_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Prices table (composite primary key: product_id + currency)
CREATE TABLE IF NOT EXISTS prices (
    product_id VARCHAR(64),
    currency VARCHAR(8),
    value DECIMAL(12,2),
    last_updated TIMESTAMP NULL,
    PRIMARY KEY (product_id, currency),
    CONSTRAINT fk_prices_product FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Price confirmations table (for write-back tracking)
CREATE TABLE IF NOT EXISTS price_confirmations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id VARCHAR(64),
    action VARCHAR(32),
    confirmed_value DECIMAL(12,2),
    currency VARCHAR(8),
    user_id VARCHAR(64),
    confirmed_at TIMESTAMP,
    synced BOOLEAN
);

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128),
    role VARCHAR(64),
    region VARCHAR(64)
);
