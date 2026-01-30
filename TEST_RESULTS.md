# Walkthrough - Debezium Consumption Rate Testing Results

I have completed the performance testing to compare Debezium CDC consumption rates with 2 and 4 concurrent threads.

## Test Configuration
- **Total Records**: 100,000 per test run.
- **Kafka Partitions**: 4 (reconfigured from 1 to enable parallel consumption).
- **Measurement Tool**: Custom `ThroughputMonitor` logging EPS (Events Per Second) every 5 seconds.

## Results Table

| Threads | Peak Throughput (msgs/sec) | Average Throughput (msgs/sec) |
| :--- | :--- | :--- |
| **2 Threads** | **3,904.20** | ~3,500 |
| **4 Threads** | **5,431.10** | ~5,000 |

## Observations & Analysis
- **Scaling Improvement**: Increasing the concurrency from 2 to 4 threads resulted in a **~39% increase** in peak consumption rate.
- **Scaling Efficiency**: While throughput improved, it was not a linear 2x scaling. This suggests that other factors (PostgreSQL WAL generation, Debezium capture lag, or single-topic partition distribution) may be becoming the next bottleneck.
- **Stability**: The application remained stable throughout high-concurrency processing (5.4k msgs/sec).

## Changes Made
1. **Infrastructure**: Updated `docker-compose.yml` to set `KAFKA_NUM_PARTITIONS: 4`.
2. **Monitoring**: Added `ThroughputMonitor.java` to track consumption speed.
3. **Load Generation**: Added `LoadGeneratorController.java` to simulate high-load scenarios.
4. **Configuration**: Parameterized `spring.kafka.listener.concurrency` and enabled scheduling.

## How to Re-run
1. Ensure Docker services are up: `docker-compose up -d`.
2. Start the application with a specific concurrency:
   ```bash
   KAFKA_CONCURRENCY=2 ./gradlew bootRun
   ```
3. Trigger the load using CURL:
   ```bash
   curl -X POST "http://localhost:8080/api/load/generate?count=100000"
   ```
4. Observe the `THROUGHPUT` logs in the console.

> [!NOTE]
> For the 4-thread test, make sure the Kafka topic (created by Debezium) has at least 4 partitions. Debezium will follow the broker's default partition count if not specified.
