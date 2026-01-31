package com.example.debezium.listener;

import com.example.debezium.model.Customer;
import com.example.debezium.monitoring.ThroughputMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomerChangeListener {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ThroughputMonitor throughputMonitor;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @KafkaListener(topics = "dbserver1.public.customers", groupId = "debezium-consumer-group")
    public void handleCustomerChange(String message, @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            org.springframework.kafka.support.Acknowledgment acknowledgment) throws Exception {

        JsonNode rootNode = objectMapper.readTree(message);

        // Extract operation type and data
        String operation = rootNode.has("__op") ? rootNode.get("__op").asText() : "UNKNOWN";
        String database = rootNode.has("__db") ? rootNode.get("__db").asText() : "N/A";
        String table = rootNode.has("__table") ? rootNode.get("__table").asText() : "N/A";
        Long timestamp = rootNode.has("__ts_ms") ? rootNode.get("__ts_ms").asLong() : null;

        // Parse the customer data
        Customer customer = objectMapper.treeToValue(rootNode, Customer.class);

        // Map operation codes to readable names
        String operationName = mapOperation(operation);

        // Log the change
        log.info("\n" + "=".repeat(80));
        log.info("[CUSTOMER CHANGE DETECTED]");
        log.info("Operation: {}", operationName);
        log.info("Database: {}", database);
        log.info("Table: {}", table);
        log.info("Partition: {}", partition);
        log.info("Timestamp: {}", timestamp != null ? java.time.Instant.ofEpochMilli(timestamp) : "N/A");
        log.info("-".repeat(80));

        if (customer != null) {
            log.info("Customer Data:");
            log.info("  ID: {}", customer.getId());
            log.info("  Name: {}", customer.getName());
            log.info("  Email: {}", customer.getEmail());
            log.info("  Created At: {}", customer.getFormattedCreatedAt());

            // Persist to capture table - ONLY capture new inserts ('c')
            if ("c".equals(operation)) {
                jdbcTemplate.update(
                        "INSERT INTO captured_customers (id, name, email) VALUES (?, ?, ?) ON CONFLICT (id) DO NOTHING",
                        customer.getId(), customer.getName(), customer.getEmail());
            }
        }

        log.info("=".repeat(80) + "\n");

        // Update metric and acknowledge ONLY if successful
        throughputMonitor.incrementCustomer();
        acknowledgment.acknowledge();
    }

    private String mapOperation(String op) {
        return switch (op) {
            case "r" -> "READ (Snapshot)";
            case "c" -> "CREATE (Insert)";
            case "u" -> "UPDATE";
            case "d" -> "DELETE";
            default -> "UNKNOWN (" + op + ")";
        };
    }
}
