package com.example.debezium.controller;

import com.example.debezium.model.Customer;
import com.example.debezium.model.Order;
import com.example.debezium.repository.CustomerRepository;
import com.example.debezium.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/load")
@RequiredArgsConstructor
public class LoadGeneratorController {

    private final JdbcTemplate jdbcTemplate;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @PostMapping("/generate")
    public String generateLoad(@RequestParam(defaultValue = "100000") int count) {
        log.info("Starting load generation: {} records for customers and orders", count);

        new Thread(() -> {
            for (int i = 0; i < count; i++) {
                try {
                    // Create and save Customer
                    Customer customer = Customer.builder()
                            .name("Customer " + i)
                            .email("customer" + i + "_" + System.currentTimeMillis() + "@example.com")
                            .build();
                    customer = customerRepository.save(customer);

                    // Create and save Order
                    Order order = Order.builder()
                            .customerId(customer.getId())
                            .productName("Product " + i)
                            .amount(new java.math.BigDecimal("99.99"))
                            .build();
                    orderRepository.save(order);

                    if (i > 0 && i % 1000 == 0) {
                        log.info("Inserted {}/{} pairs of records (sequential JPA)...", i, count);
                    }
                } catch (Exception e) {
                    log.error("Error inserting record {}: {}", i, e.getMessage());
                }
            }
            log.info("Load generation completed: {} pairs of records", count);
        }).start();

        return "Generating " + count + " records for BOTH tables sequentially in the background...";
    }

    @RequestMapping("/verify")
    public String verifyLoad() {
        Integer customerCountDb = jdbcTemplate.queryForObject("SELECT count(*) FROM customers", Integer.class);
        Integer orderCountDb = jdbcTemplate.queryForObject("SELECT count(*) FROM orders", Integer.class);

        Integer customerCountCdc = jdbcTemplate.queryForObject("SELECT count(*) FROM captured_customers",
                Integer.class);
        Integer orderCountCdc = jdbcTemplate.queryForObject("SELECT count(*) FROM captured_orders", Integer.class);

        StringBuilder report = new StringBuilder();
        report.append("--- VERIFICATION REPORT ---\n");
        report.append(
                String.format("Customers: Source DB=%d, Captured DB=%d (Diff: %d)\n", customerCountDb, customerCountCdc,
                        customerCountDb - customerCountCdc));
        report.append(String.format("Orders:    Source DB=%d, Captured DB=%d (Diff: %d)\n", orderCountDb, orderCountCdc,
                orderCountDb - orderCountCdc));

        if (customerCountDb.equals(customerCountCdc) && orderCountDb.equals(orderCountCdc)) {
            report.append("SUCCESS: All data captured by Debezium!\n");
        } else {
            report.append("FAILURE: Data loss detected!\n");
        }

        return report.toString();
    }
}
