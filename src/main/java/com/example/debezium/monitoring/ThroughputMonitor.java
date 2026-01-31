package com.example.debezium.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class ThroughputMonitor {

    private final AtomicLong customersCounter = new AtomicLong(0);
    private final AtomicLong ordersCounter = new AtomicLong(0);
    private final AtomicLong totalCounter = new AtomicLong(0);
    private long lastTime = System.currentTimeMillis();

    public void incrementCustomer() {
        customersCounter.incrementAndGet();
        totalCounter.incrementAndGet();
    }

    public void incrementOrder() {
        ordersCounter.incrementAndGet();
        totalCounter.incrementAndGet();
    }

    public long getCustomersCount() {
        return customersCounter.get();
    }

    public long getOrdersCount() {
        return ordersCounter.get();
    }

    @Scheduled(fixedRate = 5000) // Report every 5 seconds
    public void reportThroughput() {
        long total = totalCounter.get();
        long customers = customersCounter.get();
        long orders = ordersCounter.get();
        long currentTime = System.currentTimeMillis();

        log.info(">>> TOTAL PROCESSED: {} (Customers: {}, Orders: {})", total, customers, orders);
        lastTime = currentTime;
    }
}
