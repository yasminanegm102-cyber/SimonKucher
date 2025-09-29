# Pricing Application

## Overview
This backend provides intelligent room price recommendations for a hotel chain using historical booking data and demand signals. It exposes APIs for:
- Price recommendations and grouping by building
- Dynamic filter metadata for the frontend
- Multi-currency price retrieval with sorting and pagination
- Batch confirmations for accepted/overridden prices
- Algorithm configuration (target occupancy, sensitivity, window days)

In addition, the repository contains ETL jobs:
- Spring Batch CSV ingestion (local/dev)
- PySpark ETL prototypes designed for AWS Glue (cloud scale)

## Project Structure
```
pricing-app/
├── pom.xml
├── README.md
├── etl/
│   ├── pricing_etl.py       # PySpark ETL: reads CSV from S3, upserts via JDBC or writes curated Parquet
│   ├── product_clustering_pyspark.py # PySpark clustering prototype by arrival_date/room_type/beds/grade/private_pool
│   ├── pricing_mysql_schema.sql      # MySQL schema for ETL (aligned with app schema)
│   └── setup_mysql.sh
├── SCHEMA_ALIGNMENT.md              # Documentation of ETL and app schema alignment
├── src/
│   ├── main/
│   │   ├── java/com/example/pricing/
│   │   │   ├── PricingApplication.java
│   │   │   ├── config/
│   │   │   │   ├── BatchConfig.java    # Spring Batch jobs to ingest products/bookings/prices/buildings from CSV
│   │   │   │   └── SampleDataLoader.java
│   │   │   ├── controller/
│   │   │   │   ├── RecommendationController.java # Group-by-building endpoint + bookings-by-cluster
│   │   │   │   ├── FilterController.java         # Dynamic filters (buildings, room types, beds, date range)
│   │   │   │   ├── PriceController.java          # Prices: pagination, sorting, currency filter, by-product
│   │   │   │   ├── ConfirmationController.java   # Batch confirmations (accept/reject/override)
│   │   │   │   ├── AlgorithmConfigController.java# Get/put algorithm parameters
│   │   │   │   ├── BatchJobController.java
│   │   │   │   └── MetricsController.java
│   │   │   ├── dto/                              # CSV DTOs for batch ingestion
│   │   │   │   ├── ProductCsv.java
│   │   │   │   ├── BookingCsv.java
│   │   │   │   ├── PriceCsv.java
│   │   │   │   └── BuildingCsv.java
│   │   │   ├── model/                            # JPA entities
│   │   │   │   ├── Product.java
│   │   │   │   ├── Building.java
│   │   │   │   ├── Booking.java
│   │   │   │   ├── Price.java
│   │   │   │   ├── PriceId.java
│   │   │   │   ├── PriceRecommendation.java
│   │   │   │   ├── PriceConfirmation.java
│   │   │   │   └── User.java
│   │   │   ├── repository/                       # Spring Data JPA repositories
│   │   │   │   ├── ProductRepository.java
│   │   │   │   ├── BuildingRepository.java
│   │   │   │   ├── BookingRepository.java
│   │   │   │   ├── PriceRepository.java
│   │   │   │   ├── PriceRecommendationRepository.java
│   │   │   │   ├── PriceConfirmationRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   └── service/
│   │   │       ├── PricingService.java           # Pricing algorithm (cluster-based, EMA smoothing, bounds)
│   │   │       ├── ClusteringService.java        # Retrieve bookings for a cluster
│   │   │       ├── ConfirmationService.java      # Validates overrides, stores confirmations for write-back
│   │   │       ├── AlgorithmConfigService.java
│   │   │       └── SyncService.java
│   │   └── resources/
│   │       ├── application.yml                   # MySQL config, JPA settings, port
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       │   └── V1__init.sql                  # Flyway: buildings/products/bookings/prices/confirmations/users
│   │       └── static/
│   │           └── index.html
│   └── test/java/com/example/pricing/
│       └── PricingServiceTest.java               # Unit tests for pricing algorithm
└── target/                                        # Build output (generated)
```

## Setup Instructions
1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```
   cd pricing-app
   ```
3. Build the project using Maven:
   ```
   mvn clean install
   ```
4. Run the application:
   ```
   mvn spring-boot:run
   ```

## Usage
- The application can be accessed at `http://localhost:8080` by default.
- Use the provided endpoints to interact with the pricing functionalities.

## Architecture

- Spring Boot REST API
- Spring Data JPA + MySQL
- Flyway for DB migrations
- Spring Batch for CSV ingestion
- PySpark (AWS Glue-ready) for scalable ETL and clustering
- Unit tests for the core pricing algorithm



## Data Model (simplified)

- buildings(id, name, type)
- products(id, building_id FK, room_name, arrival_date, no_of_beds, room_type, grade, private_pool)
- bookings(id, product_id FK, arrival_date, nights, price_paid)
- prices(product_id, currency) PK, value, last_updated
- price_confirmations(id PK, product_id, action, confirmed_value, currency, user_id, confirmed_at, synced)
- users(id, name, role, region)

Indexes recommended:
- products(building_id)
- products(arrival_date, room_type, no_of_beds, grade, private_pool)
- bookings(product_id)
- prices(product_id, currency)

## APIs

- GET `/api/filters/{clientId}`
  - returns buildings, room types, beds, arrival date range, building types.

- GET `/api/recommendations/grouped`
  - Query params: `buildingIds`, `roomType`, `beds`, `arrivalFrom`, `arrivalTo`
  - Returns buildings with nested products and a per-product map of multi-currency prices.

- GET `/api/recommendations/bookings-by-cluster`
  - Query params: `arrivalDate`, `roomType`, `noOfBeds`, `grade`, `privatePool`
  - Returns bookings for that cluster.

- GET `/api/prices`
  - Query params: `currency?`, `page`, `size`, `sortBy`, `order`
  - Returns paginated prices with sorting and optional currency filter.

- GET `/api/prices/by-product?productId=...`
  - Returns all prices for a product across currencies.

- GET `/api/config/pricing`, PUT `/api/config/pricing`
  - Get/update `targetOccupancy`, `sensitivity`, `windowDays`.

- POST `/api/confirmations/batch`
  - Body: array of `{productId, action, price?, currency, userId}`
  - Validates override bounds (±30% of last recommendation), stores confirmations.

## Pricing Algorithm

- Cluster products by `(arrivalDate, roomType, noOfBeds, grade, privatePool)`.
- Compute occupancy over a rolling window: bookings / (products * days).
- Compute `avgPaid` and `factor = 1 + sensitivity * (occupancy - target)`.
- Clamp to `[avgPaid * minMargin, avgPaid * (1 + maxIncreasePct)]`.
- Smooth with EMA versus previous recommendation.
- Persist new recommendation if repository configured.

## ETL

- PySpark ([etl/pricing_etl.py](cci:7://file:///c:/Users/jessi/Downloads/pricing-app/pricing-app/etl/pricing_etl.py:0:0-0:0)):
  - Reads S3 CSVs, upserts via JDBC (`ON DUPLICATE KEY UPDATE`) or writes curated Parquet.
  - Glue-ready with `GlueContext` and job args.
  - **Schema aligned** with application database (see [SCHEMA_ALIGNMENT.md](SCHEMA_ALIGNMENT.md)).
- Spring Batch ([config/BatchConfig.java](cci:7://file:///c:/Users/jessi/Downloads/pricing-app/pricing-app/src/main/java/com/example/pricing/config/BatchConfig.java:0:0-0:0)):
  - Alternate CSV ingestion for local/dev environments.

## Running Locally

1. Start MySQL and create DB `pricingdb` with user/password from [application.yml](cci:7://file:///c:/Users/jessi/Downloads/pricing-app/pricing-app/src/main/resources/application.yml:0:0-0:0).
2. Build and run:
   - `mvn clean install`
   - `mvn spring-boot:run`
3. Check health at `http://localhost:8080`.
4. Import CSVs (option A, Spring Batch): configure `batch.*.file` properties and trigger batch jobs via controller (if exposed) or by running Spring Batch jobs on startup.
5. Option B (PySpark): run `spark-submit etl/pricing_etl.py` with Glue or local Spark (configure S3/JDBC).

## Tests

- [PricingServiceTest](cci:2://file:///c:/Users/jessi/Downloads/pricing-app/pricing-app/src/test/java/com/example/pricing/PricingServiceTest.java:16:0-110:1) covers key algorithm scenarios: high/low occupancy and fallback to current price.

## Notes and Next Steps
- Add indices on cluster attributes for faster cluster queries.
- Build a nightly write-back sync to hotel systems for `price_confirmations.synced=false`.
- Add endpoints to pivot multi-currency prices and sort by a selected currency if needed.