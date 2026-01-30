package com.example.debezium.monitoring;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
public class ThroughputMonitor {

    private final AtomicLong counter = new AtomicLong(0);
    private long lastTime = System.currentTimeMillis();

    public void increment() {
        counter.incrementAndGet();
    }

    @Scheduled(fixedRate = 5000) // Report every 5 seconds
    public void reportThroughput() {
        long currentCount = counter.getAndSet(0);
        long currentTime = System.currentTimeMillis();
        long durationMs = currentTime - lastTime;

        if (durationMs > 0) {
            double throughput = (currentCount * 1000.0) / durationMs;
            if (currentCount > 0) {
                log.info(">>> THROUGHPUT: {} messages processed in {}ms. Rate: {} msgs/sec",
                        currentCount, durationMs, String.format("%.2f", throughput));
            }
        }
        lastTime = currentTime;
    }
}
