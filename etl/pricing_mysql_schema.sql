-- MySQL schema for Pricing App ETL integration
-- Products table
CREATE TABLE products (
    id VARCHAR(64) PRIMARY KEY,
    room_name VARCHAR(128),
    arrival_date DATE,
    no_of_beds INT,
    room_type VARCHAR(64),
    grade INT,
    private_pool BOOLEAN,
    ingestion_date DATE
);

-- Bookings table
CREATE TABLE bookings (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64),
    creation_date DATE,
    confirmation_status VARCHAR(32),
    arrival_date DATE,
    ingestion_date DATE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Buildings table
-- Buildings table (master)
CREATE TABLE buildings (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(128),
    type VARCHAR(64)
);

-- Product-to-building linkage is in products.building_id

-- Prices table
CREATE TABLE prices (
    product_id VARCHAR(64),
    currency VARCHAR(8),
    price DECIMAL(12,2),
    ingestion_date DATE,
    PRIMARY KEY (product_id, currency),
    FOREIGN KEY (product_id) REFERENCES products(id)
);
