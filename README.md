# Hotel Pricing Recommendation Platform

## Overview
This backend provides intelligent room price recommendations for a hotel chain using historical booking data and demand signals. 

### Key Features
- ✅ **Price Recommendations** with filtering by product, product group, region, and time interval
- ✅ **Accept/Reject/Override** prices with business rule validation (±30%)
- ✅ **User Management** with role-based access control (ADMIN, REGIONAL_MANAGER, PRICING_MANAGER)
- ✅ **Regional Restrictions** - Regional managers can only manage their assigned region
- ✅ **Algorithm Configuration** - ADMIN users can adjust pricing parameters
- ✅ **Multi-Currency Support** with pagination and sorting
- ✅ **Scheduled Write-Back** - Confirmed prices sync daily at 2 AM

### ETL Capabilities
- **PySpark ETL** for AWS Glue (cloud scale, 100K+ products, 1M+ bookings)
- **Spring Batch** for local/dev CSV ingestion
- **Daily Updates** via upsert logic (handles new and updated records)

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
├── IMPLEMENTATION_SUMMARY.md        # Summary of implemented features
├── REQUIREMENTS_FULFILLMENT.md      # Detailed requirements analysis
├── src/
│   ├── main/
│   │   ├── java/com/example/pricing/
│   │   │   ├── PricingApplication.java
│   │   │   ├── config/
│   │   │   │   ├── BatchConfig.java    # Spring Batch jobs to ingest products/bookings/prices/buildings from CSV
│   │   │   │   └── SampleDataLoader.java
│   │   │   ├── controller/
│   │   │   │   ├── RecommendationController.java # Group-by-building + region/product group filters
│   │   │   │   ├── FilterController.java         # Dynamic filters (buildings, room types, beds, regions, product groups)
│   │   │   │   ├── PriceController.java          # Prices: pagination, sorting, currency filter, by-product
│   │   │   │   ├── ConfirmationController.java   # Batch confirmations (accept/reject/override)
│   │   │   │   ├── AlgorithmConfigController.java# Get/put algorithm parameters (ADMIN only)
│   │   │   │   ├── UserController.java           # User management CRUD + query by role/region
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
│   │   │       ├── ConfirmationService.java      # Validates overrides + regional restrictions
│   │   │       ├── AlgorithmConfigService.java   # Thread-safe config management
│   │   │       └── SyncService.java              # Daily write-back job (2 AM)
│   │   └── resources/
│   │       ├── application.yml                   # MySQL config, JPA settings, port
│   │       ├── application.properties
│   │       ├── db/migration/
│   │       │   ├── V1__init.sql                  # Flyway: buildings/products/bookings/prices/confirmations/users
│   │       │   └── V2__add_region_and_product_group.sql # Adds region and product_group columns
│   │       └── static/
│   │           └── index.html
│   └── test/java/com/example/pricing/
│       ├── PricingServiceTest.java               # Unit tests for pricing algorithm
│       ├── UserControllerTest.java               # User management tests (14 tests)
│       ├── ConfirmationServiceTest.java          # Regional restrictions tests (10 tests)
│       ├── AlgorithmConfigControllerTest.java    # Authorization tests (10 tests)
│       └── RecommendationControllerTest.java     # Filter tests (9 tests)
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



## Data Model

- **buildings**(id, name, type, **region**)
- **products**(id, building_id FK, room_name, arrival_date, no_of_beds, room_type, grade, private_pool, **product_group**)
- **bookings**(id, product_id FK, arrival_date, nights, price_paid)
- **prices**(product_id, currency) PK, value, last_updated
- **price_recommendations**(id PK, product_id, currency, recommended_value, recommended_at, status)
- **price_confirmations**(id PK, product_id, action, confirmed_value, currency, user_id, confirmed_at, synced)
- **users**(id, name, role, region)

Indexes recommended:
- products(building_id)
- products(arrival_date, room_type, no_of_beds, grade, private_pool)
- bookings(product_id)
- prices(product_id, currency)

## REST APIs

### Filters & Recommendations
- **GET** `/api/filters/{clientId}`
  - Returns: buildings, room types, beds, arrival date range, building types, **regions**, **product groups**

- **GET** `/api/recommendations/grouped`
  - Query params: `buildingIds`, `roomType`, `beds`, `arrivalFrom`, `arrivalTo`, **`productGroup`**, **`region`**
  - Returns: Buildings with nested products and multi-currency prices

- **GET** `/api/recommendations/bookings-by-cluster`
  - Query params: `arrivalDate`, `roomType`, `noOfBeds`, `grade`, `privatePool`
  - Returns: Bookings for that cluster

### Prices
- **GET** `/api/prices`
  - Query params: `currency?`, `page`, `size`, `sortBy`, `order`
  - Returns: Paginated prices with sorting and optional currency filter

- **GET** `/api/prices/by-product?productId=...`
  - Returns: All prices for a product across currencies

### Confirmations
- **POST** `/api/confirmations/batch`
  - Body: `[{productId, action, price?, currency, userId}]`
  - Actions: `ACCEPT`, `REJECT`, `OVERRIDE`
  - Validates: Override bounds (±30%), regional restrictions
  - Returns: Per-item status (success/failed with error message)

### Algorithm Configuration
- **GET** `/api/config/pricing`
  - Returns: Current algorithm parameters

- **PUT** `/api/config/pricing`
  - Header: `X-User-Id` (required)
  - Authorization: **ADMIN role only**
  - Body: `{targetOccupancy?, sensitivity?, windowDays?}`
  - Returns: 401 (no user), 403 (non-admin), 200 (success)

### User Management
- **GET** `/api/users` - List all users
- **GET** `/api/users/{id}` - Get user by ID
- **POST** `/api/users` - Create user (validates required fields)
- **PUT** `/api/users/{id}` - Update user
- **DELETE** `/api/users/{id}` - Delete user
- **GET** `/api/users/by-role/{role}` - Filter by role
- **GET** `/api/users/by-region/{region}` - Filter by region

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
  - **Schema aligned** with application database
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

Run all tests:
```bash
mvn test
```

### Test Coverage (46 tests, all passing ✅)
- **PricingServiceTest** (3 tests) - Core pricing algorithm
- **UserControllerTest** (14 tests) - User CRUD operations
- **ConfirmationServiceTest** (10 tests) - Regional restrictions & override validation
- **AlgorithmConfigControllerTest** (10 tests) - ADMIN authorization
- **RecommendationControllerTest** (9 tests) - Product/region/group filtering

### What's Tested
✅ Regional managers can only confirm prices in their region  
✅ Only ADMIN users can update algorithm config  
✅ Override validation (±30% bounds)  
✅ User management with validation  
✅ Multi-currency price display  
✅ Product group and region filtering  
✅ Pricing algorithm (high/low occupancy scenarios)

## Requirements Coverage

### ✅ User Story 1: Data Ingestion
- PySpark ETL with daily upserts
- AWS Glue-ready (GlueContext, DynamicFrames, S3 paths)
- Handles: Products, Bookings, Buildings, Prices CSVs
- Spring Batch alternative for local dev

### ✅ User Story 2: Product Clustering
- Cluster by: arrival_date, room_type, no_of_beds, grade, private_pool
- `ClusteringService.bookingsForCluster()` retrieves bookings
- PySpark clustering prototype for 100K+ products

### ✅ User Story 3: Dynamic Filters
- `FilterController` returns: buildings, room types, beds, date range, **regions**, **product groups**
- All values queried dynamically from database

### ✅ User Story 4: Group Products by Building
- `RecommendationController.getGrouped()` returns nested structure
- Filters by: building, room type, beds, date, **product group**, **region**
- Multi-currency price map per product

### ✅ User Story 5: Multi-Currency Display & Sorting
- `PriceController` supports pagination, sorting, currency filter
- Composite key design: (product_id, currency)
- `/by-product` endpoint returns all currencies

### ✅ Additional Requirements
- **Accept/Reject/Override** prices with validation
- **Override business rules** (±30% bounds)
- **Algorithm configuration** with ADMIN authorization
- **User management** with full CRUD
- **Regional restrictions** enforced for regional managers

## Production Readiness

✅ **46 unit tests passing**  
✅ **Flyway migrations** for version-controlled schema  
✅ **Thread-safe** algorithm configuration  
✅ **Scheduled write-back** job (daily at 2 AM)  
✅ **Regional access control** enforced  
✅ **Multi-currency** support with composite keys  
✅ **ETL schema aligned** with application schema  
✅ **Scalable** (PySpark for cloud, JPA for moderate scale)