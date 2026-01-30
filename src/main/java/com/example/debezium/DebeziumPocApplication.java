package com.example.debezium;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@org.springframework.scheduling.annotation.EnableScheduling
public class DebeziumPocApplication {

	public static void main(String[] args) {
		SpringApplication.run(DebeziumPocApplication.class, args);
		System.out.println("\n===========================================");
		System.out.println("Debezium POC Application Started!");
		System.out.println("Listening to Kafka topics:");
		System.out.println("  - dbserver1.public.customers");
		System.out.println("  - dbserver1.public.orders");
		System.out.println("===========================================\n");
	}

}
