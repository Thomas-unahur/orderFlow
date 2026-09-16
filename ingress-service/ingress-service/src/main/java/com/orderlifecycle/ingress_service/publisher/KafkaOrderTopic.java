package com.orderlifecycle.ingress_service.publisher;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaOrderTopic {

    @Bean
    public NewTopic topic() {
        return TopicBuilder.name("orders")
                .partitions(10)
                .replicas(1)
                .build();
    }

}
