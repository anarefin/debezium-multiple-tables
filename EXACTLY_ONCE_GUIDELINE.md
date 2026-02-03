# Exactly-once Delivery Guideline

This document explains how this project achieves **Exactly-once** delivery semantics instead of the default **At-least-once** behavior.

## Overview

In a standard CDC (Change Data Capture) pipeline, "At-least-once" is the default guarantee, meaning records might be duplicated during failures. To achieve "Exactly-once", every component in the chain must be configured to coordinate and prevent duplicates.

## 1. Source Side: Debezium & Kafka Connect

The foundation of exactly-once starts at the source connector.

### Kafka Connect Configuration
In `docker-compose.yml`, Kafka Connect is enabled for exactly-once source support:
```yaml
kafka-connect:
  environment:
    CONNECT_EXACTLY_ONCE_SOURCE_SUPPORT: "enabled"
```
This ensures that Kafka Connect can atomically commit source offsets and Kafka messages in a single transaction.

### Debezium Producer Settings
In `register-connectors.sh`, the Debezium connector overrides the producer settings to ensure idempotency and reliability:
```json
"producer.override.enable.idempotence": "true",
"producer.override.acks": "all"
```
- **`enable.idempotence`**: Ensures that even if the producer retries a request, it won't result in duplicate messages in the Kafka topic.
- **`acks=all`**: Ensures that the message is replicated to all in-sync replicas before being acknowledged.

## 2. Infrastructure: Kafka Broker

The Kafka broker must support transactions and idempotency.
- **`KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 1`** and **`KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 1`** (configured in `docker-compose.yml` for this POC) are used to manage transaction state. In production, these should be higher (usually 3).

## 3. Sink Side: Spring Boot Consumer

To complete the exactly-once chain, the consumer must only read committed data and handle offsets manually or through transactions.

### Consumer Configuration
In `application.yml`, the consumer is configured with:
```yaml
spring:
  kafka:
    consumer:
      isolation-level: read_committed
      enable-auto-commit: false
    listener:
      ack-mode: manual
```
- **`isolation-level: read_committed`**: Ensures the consumer ignores messages from aborted transactions.
- **`enable-auto-commit: false`**: Prevents Kafka from automatically committing offsets at intervals, which can lead to data loss or duplicates during failures.

### Idempotent Processing
Even with infrastructure guarantees, the application logic should be idempotent. In `CustomerChangeListener.java`:
```java
jdbcTemplate.update(
    "INSERT INTO captured_customers (id, name, email) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
    customer.getId(), customer.getName(), customer.getEmail());

// ...

acknowledgment.acknowledge();
```
- **`ON CONFLICT (id) DO NOTHING`**: This SQL logic ensures that if the same record is processed twice (e.g., due to a crash between the DB write and the Kafka ACK), the second attempt will be ignored by the database.
- **Manual Acknowledgment**: The offset is only committed to Kafka *after* the database operation is successful.

## Summary: The Exactly-once Chain

1.  **Source Connector** (Debezium + Connect) uses transactions to write to Kafka.
2.  **Kafka Producer** uses idempotence to prevent duplicates during retries.
3.  **Kafka Consumer** reads only committed messages (`read_committed`).
4.  **Application Logic** uses idempotent database writes (`ON CONFLICT`).
5.  **Manual ACKs** ensure the offset is only advanced after successful processing.
