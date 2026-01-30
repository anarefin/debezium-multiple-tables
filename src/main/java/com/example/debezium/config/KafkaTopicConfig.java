package com.example.debezium.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.partitions:4}")
    private int partitions;

    @Bean
    public NewTopic customersTopic() {
        return TopicBuilder.name("dbserver1.public.customers")
                .partitions(partitions)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name("dbserver1.public.orders")
                .partitions(partitions)
                .replicas(1)
                .build();
    }
}
