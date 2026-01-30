# Quick Start Guide

Get the Debezium POC running in 3 simple steps!

## Prerequisites

- Docker and Docker Compose installed
- JDK 21 installed
- Ports 5432, 9092, 2181, 8083, and 8080 available

## Quick Start

### Option 1: Automated Setup (Recommended)

Run the automated setup script:

```bash
./run-poc.sh
```

Then in a new terminal:

```bash
./gradlew bootRun
```

### Option 2: Manual Setup

**Terminal 1 - Infrastructure Setup:**

```bash
# Start all services
docker-compose up -d

# Wait 30 seconds for services to start
sleep 30

# Register Debezium connector
./debezium-connector/register-connectors.sh
```

**Terminal 2 - Run Spring Boot App:**

```bash
./gradlew bootRun
```

## Test It

**Terminal 3 - Insert Test Data:**

```bash
docker exec -it postgres psql -U postgres -d debezium_db
```

Then run these SQL commands:

```sql
-- Insert a new customer
INSERT INTO customers (name, email, created_at) 
VALUES ('Test User', 'test@example.com', NOW());

-- Insert a new order
INSERT INTO orders (customer_id, product_name, amount, order_date) 
VALUES (1, 'Test Product', 99.99, NOW());

-- View all customers
SELECT * FROM customers;

-- View all orders
SELECT * FROM orders;

-- Exit
\q
```

## What to Expect

You should see formatted logs in Terminal 2 (Spring Boot) like:

```
================================================================================
[CUSTOMER CHANGE DETECTED]
Operation: CREATE (Insert)
Database: debezium_db
Table: customers
Timestamp: 2026-01-28T...
--------------------------------------------------------------------------------
Customer Data:
  ID: 5
  Name: Test User
  Email: test@example.com
  Created At: 2026-01-28T...
================================================================================
```

## Troubleshooting

### Connector not registered?

```bash
curl http://localhost:8083/connectors/postgres-connector/status | jq .
```

Should show `"state": "RUNNING"`

### No logs in Spring Boot?

Check Kafka topics:

```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

Should see:
- `dbserver1.public.customers`
- `dbserver1.public.orders`

### View messages in Kafka directly:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.public.customers \
  --from-beginning --max-messages 5
```

## Cleanup

```bash
# Stop Spring Boot (Ctrl+C in Terminal 2)

# Stop Docker services
docker-compose down

# Remove all data (optional)
docker-compose down -v
```

## Next Steps

See [README.md](README.md) for:
- Detailed architecture
- Full documentation
- Advanced testing scenarios
- Monitoring commands
- Troubleshooting guide
