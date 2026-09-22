package com.orderlifecycle.ingress_service.PublisherTests;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import com.orderlifecycle.ingress_service.dto.OrderEvent;
import com.orderlifecycle.ingress_service.dto.enums.OrderStatus;
import com.orderlifecycle.ingress_service.dto.enums.OrderType;
import com.orderlifecycle.ingress_service.dto.enums.Side;
import com.orderlifecycle.ingress_service.publisher.OrderPublisher;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderPublisher Unit Tests")
public class OrderPublisherTest {

    private static final String EXPECTED_TOPIC = "orders";
    private static final String DEFAULT_SYMBOL = "MSFT";
    private static final String DEFAULT_USER_ID = "user-123";
    private static final BigDecimal DEFAULT_QUANTITY = new BigDecimal("10.00");
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("400.00");

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @InjectMocks
    private OrderPublisher orderPublisher;

    @Test
    @DisplayName("Should send OrderEvent to Kafka topic 'orders' using symbol as partitioning key")
    public void publishOrder_sendsEventToKafkaWithSymbolAsKey() {
        // Arrange
        OrderEvent event = new OrderEvent(
                UUID.randomUUID(),
                DEFAULT_USER_ID,
                DEFAULT_SYMBOL,
                Side.BUY,
                OrderType.LIMIT,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                OrderStatus.PENDING,
                System.nanoTime()
        );

        // Act
        orderPublisher.publishOrder(event);

        // Assert
        verify(kafkaTemplate).send(EXPECTED_TOPIC, DEFAULT_SYMBOL, event);
        verifyNoMoreInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("Should propagate exception when KafkaTemplate fails to send message")
    public void publishOrder_whenKafkaTemplateFails_propagatesException() {
        // Arrange
        OrderEvent event = new OrderEvent(
                UUID.randomUUID(),
                DEFAULT_USER_ID,
                DEFAULT_SYMBOL,
                Side.BUY,
                OrderType.LIMIT,
                DEFAULT_QUANTITY,
                DEFAULT_PRICE,
                OrderStatus.PENDING,
                System.nanoTime()
        );

        when(kafkaTemplate.send(EXPECTED_TOPIC, DEFAULT_SYMBOL, event))
                .thenThrow(new RuntimeException("Kafka broker unreachable"));

        // Act & Assert
        assertThrows(
                RuntimeException.class,
                () -> orderPublisher.publishOrder(event)
        );
    }
}
