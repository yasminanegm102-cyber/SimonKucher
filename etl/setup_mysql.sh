#!/bin/bash
# Automated MySQL database setup for Pricing App
# Usage: ./setup_mysql.sh <MYSQL_USER> <MYSQL_PASSWORD> <MYSQL_DB>

MYSQL_USER=$root
MYSQL_PASSWORD=$password
MYSQL_DB=$pricing_db

# Create database and tables
mysql -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" <<EOF
CREATE DATABASE IF NOT EXISTS $MYSQL_DB;
USE $MYSQL_DB;

-- Products table
CREATE TABLE IF NOT EXISTS products (
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
CREATE TABLE IF NOT EXISTS bookings (
    id VARCHAR(64) PRIMARY KEY,
    product_id VARCHAR(64),
    creation_date DATE,
    confirmation_status VARCHAR(32),
    arrival_date DATE,
    ingestion_date DATE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Buildings table
CREATE TABLE IF NOT EXISTS buildings (
    building VARCHAR(128),
    product_id VARCHAR(64),
    ingestion_date DATE,
    PRIMARY KEY (building, product_id),
    FOREIGN KEY (product_id) REFERENCES products(id)
);

-- Prices table
CREATE TABLE IF NOT EXISTS prices (
    product_id VARCHAR(64) PRIMARY KEY,
    price DECIMAL(12,2),
    currency VARCHAR(8),
    ingestion_date DATE,
    FOREIGN KEY (product_id) REFERENCES products(id)
);
EOF
