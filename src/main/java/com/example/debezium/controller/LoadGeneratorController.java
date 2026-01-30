package com.example.debezium.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class LoadGeneratorController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/generate")
    public String generateLoad(@RequestParam(defaultValue = "10000") int count) {
        log.info("Starting load generation: {} records", count);

        new Thread(() -> {
            int batchSize = 1000;
            for (int i = 0; i < count; i += batchSize) {
                int currentBatch = Math.min(batchSize, count - i);
                List<Object[]> batchArgs = new ArrayList<>();
                for (int j = 0; j < currentBatch; j++) {
                    int id = i + j;
                    batchArgs.add(new Object[] { "User " + id,
                            "user" + id + "_" + System.currentTimeMillis() + "@example.com" });
                }
                jdbcTemplate.batchUpdate("INSERT INTO customers (name, email) VALUES (?, ?)", batchArgs);
                if (i % 10000 == 0) {
                    log.info("Inserted {}/{} records...", i + currentBatch, count);
                }
            }
            log.info("Load generation completed: {} records", count);
        }).start();

        return "Generating " + count + " records in the background...";
    }
}
