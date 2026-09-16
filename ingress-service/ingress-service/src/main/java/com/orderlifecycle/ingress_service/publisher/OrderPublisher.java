package com.orderlifecycle.ingress_service.publisher;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import com.orderlifecycle.ingress_service.dto.OrderEvent;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OrderPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "orders";

    public void publishOrder(OrderEvent event) {
        kafkaTemplate.send(TOPIC, event.symbol(), event);
    }
}
