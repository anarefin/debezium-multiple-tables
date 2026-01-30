package com.example.debezium.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Customer {
    
    private Long id;
    
    private String name;
    
    private String email;
    
    @JsonProperty("created_at")
    private Long createdAt; // Debezium sends timestamps as microseconds since epoch
    
    public String getFormattedCreatedAt() {
        if (createdAt != null) {
            return Instant.ofEpochMilli(createdAt / 1000).toString();
        }
        return null;
    }
}
