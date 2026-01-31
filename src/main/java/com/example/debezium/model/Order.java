package com.example.debezium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("customer_id")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @JsonProperty("product_name")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @JsonProperty("order_date")
    @Column(name = "order_date", insertable = false, updatable = false)
    private Long orderDate; // Debezium sends timestamps as microseconds since epoch

    public String getFormattedOrderDate() {
        if (orderDate != null) {
            return Instant.ofEpochMilli(orderDate / 1000).toString();
        }
        return null;
    }
}
