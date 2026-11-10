package com.orderlifecycle.ingress_service.publisher;

import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import com.orderlifecycle.ingress_service.dto.OrderEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderPublisher {
    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;
    private static final String TOPIC = "orders";

    public void publishOrder(OrderEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.symbol(), event).get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("Fallo al persistir la order {} en Kafka", event.userId(), e);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Broker temporalmente inaccesible");
        }

    }
}
