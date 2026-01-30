package com.example.debezium.listener;

import com.example.debezium.model.Order;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderChangeListener {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @KafkaListener(topics = "dbserver1.public.orders", groupId = "debezium-consumer-group")
    public void handleOrderChange(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);
            
            // Extract operation type and data
            String operation = rootNode.has("__op") ? rootNode.get("__op").asText() : "UNKNOWN";
            String database = rootNode.has("__db") ? rootNode.get("__db").asText() : "N/A";
            String table = rootNode.has("__table") ? rootNode.get("__table").asText() : "N/A";
            Long timestamp = rootNode.has("__ts_ms") ? rootNode.get("__ts_ms").asLong() : null;
            
            // Parse the order data
            Order order = objectMapper.treeToValue(rootNode, Order.class);
            
            // Map operation codes to readable names
            String operationName = mapOperation(operation);
            
            // Log the change
            log.info("\n" + "=".repeat(80));
            log.info("[ORDER CHANGE DETECTED]");
            log.info("Operation: {}", operationName);
            log.info("Database: {}", database);
            log.info("Table: {}", table);
            log.info("Timestamp: {}", timestamp != null ? java.time.Instant.ofEpochMilli(timestamp) : "N/A");
            log.info("-".repeat(80));
            
            if (order != null) {
                log.info("Order Data:");
                log.info("  ID: {}", order.getId());
                log.info("  Customer ID: {}", order.getCustomerId());
                log.info("  Product Name: {}", order.getProductName());
                log.info("  Amount: ${}", order.getAmount());
                log.info("  Order Date: {}", order.getFormattedOrderDate());
            }
            
            log.info("=".repeat(80) + "\n");
            
        } catch (Exception e) {
            log.error("Error processing order change event: {}", e.getMessage(), e);
            log.error("Raw message: {}", message);
        }
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
