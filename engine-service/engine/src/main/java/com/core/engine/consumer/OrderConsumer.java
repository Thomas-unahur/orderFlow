package com.core.engine.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.core.engine.dto.OrderEvent;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class OrderConsumer {

    @KafkaListener(topics = "${app.kafka.topics.orders}", groupId = "${spring.kafka.consumer.group-id}")
    public void consume(ConsumerRecord<String, OrderEvent> record, Acknowledgment ack) {

        OrderEvent event = record.value();
        String symbolKey = record.key();
        int partition = record.partition();
        long offset = record.offset();

        log.info("Evento recibido -> Tópico: {}, Partición: {}, Offset: {}, Clave: {}, OrderId: {}",
                record.topic(), partition, offset, symbolKey, event.orderId());

        try {
            processOrder(event);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("Fallo crítico procesando la orden {}. Offset {} retenido", event.orderId(), offset, ex);
            throw ex;
        }
    }

    private void processOrder(OrderEvent event) {
        // TODO fase 4
    }
}
