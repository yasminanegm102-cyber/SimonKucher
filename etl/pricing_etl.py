# PySpark ETL for Pricing App
# User Story 1: Data Ingestion
# - Reads Products, Bookings, Buildings, Prices CSVs
# - Handles daily updates (upserts)
# - Writes to target database (e.g., PostgreSQL, MySQL, or AWS RDS)
# - Designed for AWS Glue deployment

from pyspark.sql import SparkSession
from pyspark.sql.functions import col, current_date
from pyspark.sql.types import *

# If running on AWS Glue
try:
    from awsglue.context import GlueContext
    from awsglue.job import Job
    from awsglue.utils import getResolvedOptions
    glue_available = True
except Exception:
    glue_available = False

# Define schemas for each input (aligned to application tables)
product_schema = StructType([
    StructField("id", StringType(), True),
    StructField("building_id", StringType(), True),
    StructField("room_name", StringType(), True),
    StructField("arrival_date", DateType(), True),
    StructField("no_of_beds", IntegerType(), True),
    StructField("room_type", StringType(), True),
    StructField("grade", IntegerType(), True),
    StructField("private_pool", BooleanType(), True)
])

booking_schema = StructType([
    StructField("id", StringType(), True),
    StructField("product_id", StringType(), True),
    StructField("arrival_date", DateType(), True),
    StructField("nights", IntegerType(), True),
    StructField("price_paid", DoubleType(), True)
])

building_schema = StructType([
    StructField("id", StringType(), True),
    StructField("name", StringType(), True),
    StructField("type", StringType(), True)
])

price_schema = StructType([
    StructField("product_id", StringType(), True),
    StructField("currency", StringType(), True),
    StructField("value", DoubleType(), True),
    StructField("last_updated", TimestampType(), True)
])

if glue_available:
    import sys
    args = getResolvedOptions(sys.argv, ["JOB_NAME", "JDBC_URL", "JDBC_USER", "JDBC_PASSWORD", "S3_BUCKET"])  # add more if needed
    spark = SparkSession.builder.appName("PricingETL").getOrCreate()
    glueContext = GlueContext(spark.sparkContext)
    job = Job(glueContext)
    job.init(args["JOB_NAME"], args)
    s3_bucket = args["S3_BUCKET"]
    jdbc_url = args["JDBC_URL"]
    jdbc_user = args["JDBC_USER"]
    jdbc_password = args["JDBC_PASSWORD"]
else:
    spark = SparkSession.builder.appName("PricingETL").getOrCreate()
    glueContext = None
    job = None
    s3_bucket = "your-bucket"
    jdbc_url = None
    jdbc_user = None
    jdbc_password = None

# Paths to input files (replace with S3 paths in Glue)
products_path = f"s3://{s3_bucket}/landing/products.csv"
bookings_path = f"s3://{s3_bucket}/landing/bookings.csv"
buildings_path = f"s3://{s3_bucket}/landing/buildings.csv"
prices_path = f"s3://{s3_bucket}/landing/prices.csv"

def read_csv(path, schema):
    return spark.read.option("header", True).schema(schema).csv(path)

# Read input data (use DynamicFrames for Glue bookmarks if available)
if glue_available:
    products_dyf = glueContext.create_dynamic_frame.from_options(connection_type="s3", connection_options={"paths": [products_path], "recurse": True, "groupFiles": "inPartition", "groupSize": "1048576"}, format="csv", format_options={"withHeader": True})
    bookings_dyf = glueContext.create_dynamic_frame.from_options(connection_type="s3", connection_options={"paths": [bookings_path], "recurse": True}, format="csv", format_options={"withHeader": True})
    buildings_dyf = glueContext.create_dynamic_frame.from_options(connection_type="s3", connection_options={"paths": [buildings_path], "recurse": True}, format="csv", format_options={"withHeader": True})
    prices_dyf = glueContext.create_dynamic_frame.from_options(connection_type="s3", connection_options={"paths": [prices_path], "recurse": True}, format="csv", format_options={"withHeader": True})
    products_df = products_dyf.toDF()
    bookings_df = bookings_dyf.toDF()
    buildings_df = buildings_dyf.toDF()
    prices_df = prices_dyf.toDF()
else:
    products_df = read_csv(products_path, product_schema)
    bookings_df = read_csv(bookings_path, booking_schema)
    buildings_df = read_csv(buildings_path, building_schema)
    prices_df = read_csv(prices_path, price_schema)

# Optional: add ingestion date columns to staging writes if desired

def upsert_to_db(df, table_name, key_columns):
    if jdbc_url:
        # Write to temp stage and MERGE using JDBC (database-specific SQL)
        tmp_view = f"tmp_{table_name}"
        df.createOrReplaceTempView(tmp_view)
        # For portability, perform row-by-row upsert using foreachPartition and JDBC
        def merge_partition(rows):
            import sqlalchemy
            from sqlalchemy import text
            engine = sqlalchemy.create_engine(jdbc_url)
            with engine.begin() as conn:
                for r in rows:
                    data = r.asDict()
                    if table_name == "prices":
                        # composite key: product_id + currency
                        stmt = text("""
                            INSERT INTO prices (product_id, currency, value, last_updated)
                            VALUES (:product_id, :currency, :value, :last_updated)
                            ON DUPLICATE KEY UPDATE value = VALUES(value), last_updated = VALUES(last_updated)
                        """)
                    else:
                        # single-key upsert by id when applicable
                        key = key_columns[0]
                        cols = ",".join(data.keys())
                        placeholders = ":" + ",:".join(data.keys())
                        update_clause = ",".join([f"{c}=VALUES({c})" for c in data.keys() if c != key])
                        stmt = text(f"INSERT INTO {table_name} ({cols}) VALUES ({placeholders}) ON DUPLICATE KEY UPDATE {update_clause}")
                    conn.execute(stmt, **data)
        df.foreachPartition(merge_partition)
    else:
        # Fallback to curated zone in S3 when no JDBC provided
        df.write.mode("overwrite").parquet(f"s3://{s3_bucket}/curated/{table_name}")

# Upsert each table
datasets = [
    (buildings_df, "buildings", ["id"]),
    (products_df, "products", ["id"]),
    (bookings_df, "bookings", ["id"]),
    (prices_df, "prices", ["product_id", "currency"])
]
for df, name, keys in datasets:
    upsert_to_db(df, name, keys)
if job:
    job.commit()
spark.stop()
