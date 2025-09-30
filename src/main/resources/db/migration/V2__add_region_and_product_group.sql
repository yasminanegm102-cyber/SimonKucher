-- Add region column to buildings table
ALTER TABLE buildings ADD COLUMN region VARCHAR(64);

-- Add product_group column to products table
ALTER TABLE products ADD COLUMN product_group VARCHAR(64);
