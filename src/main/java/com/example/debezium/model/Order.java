package com.example.debezium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {
    
    private Long id;
    
    @JsonProperty("customer_id")
    private Long customerId;
    
    @JsonProperty("product_name")
    private String productName;
    
    private BigDecimal amount;
    
    @JsonProperty("order_date")
    private Long orderDate; // Debezium sends timestamps as microseconds since epoch
    
    public String getFormattedOrderDate() {
        if (orderDate != null) {
            return Instant.ofEpochMilli(orderDate / 1000).toString();
        }
        return null;
    }
}
