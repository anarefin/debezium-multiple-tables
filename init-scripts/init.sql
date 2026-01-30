-- Create customers table
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_id INTEGER NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(id)
);

-- Insert sample data for customers
INSERT INTO customers (name, email, created_at) VALUES
    ('John Doe', 'john.doe@example.com', NOW()),
    ('Alice Smith', 'alice.smith@example.com', NOW()),
    ('Bob Johnson', 'bob.johnson@example.com', NOW());

-- Insert sample data for orders
INSERT INTO orders (customer_id, product_name, amount, order_date) VALUES
    (1, 'iPhone 15 Pro', 1199.99, NOW()),
    (1, 'AirPods Pro', 249.99, NOW()),
    (2, 'MacBook Pro M3', 2499.99, NOW()),
    (3, 'iPad Air', 599.99, NOW());

-- Grant necessary permissions for Debezium
ALTER TABLE customers REPLICA IDENTITY FULL;
ALTER TABLE orders REPLICA IDENTITY FULL;

-- Display initial data
SELECT 'Customers table:' AS info;
SELECT * FROM customers;

SELECT 'Orders table:' AS info;
SELECT * FROM orders;
