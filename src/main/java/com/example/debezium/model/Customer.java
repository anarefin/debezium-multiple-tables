package com.example.debezium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonProperty("created_at")
    @Column(name = "created_at", insertable = false, updatable = false)
    private Long createdAt; // Debezium sends timestamps as microseconds since epoch

    public String getFormattedCreatedAt() {
        if (createdAt != null) {
            return Instant.ofEpochMilli(createdAt / 1000).toString();
        }
        return null;
    }
}
