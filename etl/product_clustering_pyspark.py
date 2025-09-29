# PySpark module for product clustering and booking retrieval
# User Story 2: Product Clustering in PySpark
# - Clusters products by arrival_date, room_type, no_of_beds, grade, private_pool
# - Retrieves associated bookings for each cluster
# - Scalable for large datasets (100K products, 1M bookings)

from pyspark.sql import SparkSession
from pyspark.sql.functions import col

# Initialize Spark session (Glue job will provide this)
spark = SparkSession.builder.appName("ProductClustering").getOrCreate()

# Paths to input files (replace with S3 paths in Glue)
products_path = "s3://your-bucket/products.csv"
bookings_path = "s3://your-bucket/bookings.csv"

# Read input data
products_df = spark.read.option("header", True).csv(products_path)
bookings_df = spark.read.option("header", True).csv(bookings_path)

# Cast columns to correct types (if needed)
products_df = products_df.withColumn("no_of_beds", col("no_of_beds").cast("int")) \
    .withColumn("grade", col("grade").cast("int")) \
    .withColumn("private_pool", col("private_pool").cast("boolean"))

# Define cluster key columns
cluster_cols = ["arrival_date", "room_type", "no_of_beds", "grade", "private_pool"]

# Assign cluster key to products
enriched_products = products_df.withColumn(
    "cluster_key",
    col("arrival_date") + "_" + col("room_type") + "_" + col("no_of_beds").cast("string") + "_" + col("grade").cast("string") + "_" + col("private_pool").cast("string")
)

# Join bookings to products to get cluster for each booking
bookings_with_cluster = bookings_df.join(
    enriched_products.select("id", "cluster_key"),
    bookings_df.product_id == enriched_products.id,
    how="left"
)

# Group bookings by cluster
bookings_by_cluster = bookings_with_cluster.groupBy("cluster_key").agg(
    # Example: collect booking ids, count, etc.
    # F.collect_list("id").alias("booking_ids"),
    {"id": "count"}
)

# Save or return the result as needed (e.g., write to S3 or database)
bookings_by_cluster.write.mode("overwrite").parquet("s3://your-bucket/curated/bookings_by_cluster")

spark.stop()
