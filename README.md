# Debezium Multiple Tables POC

A Proof of Concept demonstrating how Debezium can capture changes from 2 PostgreSQL tables (`customers` and `orders`) using Change Data Capture (CDC) and stream them to Kafka, with a Spring Boot application consuming and logging the events.

## Architecture

```
PostgreSQL (17) → Debezium Connector (2.7) → Kafka → Spring Boot 3.5 Consumer
```

- **PostgreSQL 17**: Source database with WAL-based CDC
- **Zookeeper**: Coordination service for Kafka
- **Kafka**: Message broker for streaming change events
- **Kafka Connect**: Runtime environment for Debezium connector
- **Spring Boot 3.5**: Consumer application with JDK 21

## Prerequisites

- Docker and Docker Compose
- JDK 21 (for running Spring Boot locally)
- curl and jq (for connector registration and status checks)

## Project Structure

```
debezium-multiple-tables/
├── docker-compose.yml              # Docker services configuration
├── init-scripts/
│   └── init.sql                    # Database initialization script
├── debezium-connector/
│   └── register-connectors.sh      # Debezium connector registration script
├── build.gradle                    # Gradle build configuration
├── settings.gradle                 # Gradle settings
├── src/main/
│   ├── java/com/example/debezium/
│   │   ├── DebeziumPocApplication.java          # Main application
│   │   ├── listener/
│   │   │   ├── CustomerChangeListener.java      # Customer events listener
│   │   │   └── OrderChangeListener.java         # Order events listener
│   │   └── model/
│   │       ├── Customer.java                    # Customer model
│   │       └── Order.java                       # Order model
│   └── resources/
│       └── application.yml         # Spring Boot configuration
└── README.md                       # This file
```

## Getting Started

### Step 1: Start Docker Services

Start all required services (PostgreSQL, Zookeeper, Kafka, Kafka Connect):

```bash
docker-compose up -d
```

This will:
- Start PostgreSQL 17 with logical replication enabled
- Initialize the database with `customers` and `orders` tables
- Start Kafka and Zookeeper
- Start Kafka Connect with Debezium

Wait for all services to be healthy (~30-60 seconds):

```bash
docker-compose ps
```

All services should show "healthy" status.

### Step 2: Register Debezium Connector

Make the registration script executable and run it:

```bash
chmod +x debezium-connector/register-connectors.sh
./debezium-connector/register-connectors.sh
```

This script will:
- Wait for Kafka Connect to be ready
- Register the PostgreSQL connector
- Configure it to capture changes from `customers` and `orders` tables
- Display the connector status

**Verify connector is running:**

```bash
curl -s http://localhost:8083/connectors/postgres-connector/status | jq .
```

You should see `"state": "RUNNING"` in the output.

**Check Kafka topics:**

```bash
docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
```

You should see:
- `dbserver1.public.customers`
- `dbserver1.public.orders`

### Step 3: Build and Run Spring Boot Application

Build the application:

```bash
./gradlew clean build
```

Run the application:

```bash
./gradlew bootRun
```

You should see:
```
===========================================
Debezium POC Application Started!
Listening to Kafka topics:
  - dbserver1.public.customers
  - dbserver1.public.orders
===========================================
```

The application will immediately start processing the initial snapshot of existing data.

## Testing the POC

### Test 1: Insert New Customer

Open a new terminal and connect to PostgreSQL:

```bash
docker exec -it postgres psql -U postgres -d debezium_db
```

Insert a new customer:

```sql
INSERT INTO customers (name, email, created_at) 
VALUES ('Jane Smith', 'jane.smith@example.com', NOW());
```

**Expected Output** in Spring Boot console:

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
  Name: Jane Smith
  Email: jane.smith@example.com
  Created At: 2026-01-28T...
================================================================================
```

### Test 2: Insert New Order

While still in the PostgreSQL terminal:

```sql
INSERT INTO orders (customer_id, product_name, amount, order_date) 
VALUES (1, 'Samsung Galaxy S24', 899.99, NOW());
```

**Expected Output** in Spring Boot console:

```
================================================================================
[ORDER CHANGE DETECTED]
Operation: CREATE (Insert)
Database: debezium_db
Table: orders
Timestamp: 2026-01-28T...
--------------------------------------------------------------------------------
Order Data:
  ID: 5
  Customer ID: 1
  Product Name: Samsung Galaxy S24
  Amount: $899.99
  Order Date: 2026-01-28T...
================================================================================
```

### Test 3: Update Existing Record

Update a customer:

```sql
UPDATE customers SET email = 'john.updated@example.com' WHERE id = 1;
```

You should see an UPDATE operation logged in the Spring Boot console.

### Test 4: Delete Record

Delete an order:

```sql
DELETE FROM orders WHERE id = 1;
```

You should see a DELETE operation logged in the Spring Boot console.

### Additional SQL Commands

View all customers:
```sql
SELECT * FROM customers;
```

View all orders:
```sql
SELECT * FROM orders;
```

Exit PostgreSQL:
```sql
\q
```

## Monitoring and Debugging

### Check Kafka Connect Status

```bash
curl http://localhost:8083/connectors
curl http://localhost:8083/connectors/postgres-connector/status
```

### View Kafka Messages Directly

Consumer messages from customers topic:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.public.customers \
  --from-beginning \
  --max-messages 5
```

Consumer messages from orders topic:

```bash
docker exec -it kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic dbserver1.public.orders \
  --from-beginning \
  --max-messages 5
```

### View Docker Logs

PostgreSQL logs:
```bash
docker logs postgres
```

Kafka Connect logs:
```bash
docker logs kafka-connect
```

Kafka logs:
```bash
docker logs kafka
```

### Check PostgreSQL Replication Slot

Connect to PostgreSQL and check the replication slot:

```bash
docker exec -it postgres psql -U postgres -d debezium_db
```

```sql
SELECT * FROM pg_replication_slots;
```

You should see a slot named `debezium`.

## Cleanup

### Stop Spring Boot Application

Press `Ctrl+C` in the terminal running the Spring Boot app.

### Stop and Remove Docker Containers

```bash
docker-compose down
```

### Remove Docker Volumes (Complete Cleanup)

```bash
docker-compose down -v
```

This will remove all data including the PostgreSQL database.

## Troubleshooting

### Connector Not Starting

If the connector fails to start:

1. Check Kafka Connect logs:
   ```bash
   docker logs kafka-connect
   ```

2. Verify PostgreSQL is configured for logical replication:
   ```bash
   docker exec -it postgres psql -U postgres -c "SHOW wal_level;"
   ```
   Should return `logical`.

3. Check connector configuration:
   ```bash
   curl http://localhost:8083/connectors/postgres-connector/config | jq .
   ```

### No Messages in Spring Boot

1. Verify Kafka topics exist:
   ```bash
   docker exec -it kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```

2. Check if messages are in Kafka:
   ```bash
   docker exec -it kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic dbserver1.public.customers \
     --from-beginning --max-messages 1
   ```

3. Verify Spring Boot application.yml has correct Kafka bootstrap server.

### Port Conflicts

If you get port binding errors:

- PostgreSQL: 5432
- Kafka: 9092
- Zookeeper: 2181
- Kafka Connect: 8083
- Spring Boot: 8080

Stop any services using these ports or modify `docker-compose.yml` to use different ports.

## Key Features Demonstrated

1. **Change Data Capture (CDC)**: Captures INSERT, UPDATE, and DELETE operations
2. **Multiple Tables**: Monitors 2 tables simultaneously (`customers` and `orders`)
3. **Real-time Streaming**: Changes appear in Kafka within milliseconds
4. **Initial Snapshot**: Captures existing data when connector starts
5. **Structured Logging**: Clear, formatted logs for each change event
6. **Event Metadata**: Includes operation type, database, table, and timestamp
7. **Production Patterns**: Uses Kafka Connect, consumer groups, and proper error handling

## Technology Stack

- **Spring Boot**: 3.5.0
- **JDK**: 21
- **PostgreSQL**: 17
- **Debezium**: 2.7
- **Kafka**: 7.5.0 (Confluent)
- **Docker Compose**: 3.8

## Notes

- The Debezium connector uses the `ExtractNewRecordState` transformation to unwrap the change event envelope, making the data easier to work with
- PostgreSQL's `REPLICA IDENTITY FULL` is set on both tables to capture complete row data for updates and deletes
- The connector uses `pgoutput` plugin (PostgreSQL's native logical replication output plugin)
- Snapshot mode is set to `initial`, meaning existing data is captured when the connector first starts

## License

This is a POC project for demonstration purposes.
