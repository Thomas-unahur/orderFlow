package com.core.engine.consumer;

import java.math.BigDecimal;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import com.core.engine.dto.OrderEvent;
import com.core.engine.dto.enums.OrderStatus;
import com.core.engine.dto.enums.OrderType;
import com.core.engine.dto.enums.Side;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderConsumer Unit Tests")
public class OrderConsumerTest {

        private static final String DEFAULT_TOPIC = "orders";
        private static final int DEFAULT_PARTITION = 0;
        private static final long DEFAULT_OFFSET = 100L;
        private static final String DEFAULT_SYMBOL = "MSFT";
        private static final String DEFAULT_USER_ID = "usr-test-123";
        private static final BigDecimal DEFAULT_QUANTITY = new BigDecimal("10.00");
        private static final BigDecimal DEFAULT_PRICE = new BigDecimal("400.00");

        @Mock
        private Acknowledgment acknowledgment;

        @InjectMocks
        private OrderConsumer orderConsumer;

        @ParameterizedTest(name = "Side: {0}")
        @EnumSource(Side.class)
        @DisplayName("Should process OrderEvent and acknowledge Kafka offset for all order sides")
        public void consume_validOrderEventAllSides_acknowledgesOffset(Side side) {
                // Arrange
                OrderEvent event = createOrderEvent(side, OrderType.LIMIT);
                ConsumerRecord<String, OrderEvent> record = new ConsumerRecord<>(
                                DEFAULT_TOPIC,
                                DEFAULT_PARTITION,
                                DEFAULT_OFFSET,
                                DEFAULT_SYMBOL,
                                event);

                // Act
                orderConsumer.consume(record, acknowledgment);

                // Assert
                verify(acknowledgment).acknowledge();
                verifyNoMoreInteractions(acknowledgment);
        }

        @ParameterizedTest(name = "OrderType: {0}")
        @EnumSource(OrderType.class)
        @DisplayName("Should process OrderEvent and acknowledge Kafka offset for all order types")
        public void consume_validOrderEventAllOrderTypes_acknowledgesOffset(OrderType type) {
                // Arrange
                OrderEvent event = createOrderEvent(Side.BUY, type);
                ConsumerRecord<String, OrderEvent> record = new ConsumerRecord<>(
                                DEFAULT_TOPIC,
                                DEFAULT_PARTITION,
                                DEFAULT_OFFSET,
                                DEFAULT_SYMBOL,
                                event);

                // Act
                orderConsumer.consume(record, acknowledgment);

                // Assert
                verify(acknowledgment).acknowledge();
                verifyNoMoreInteractions(acknowledgment);
        }

        @Test
        @DisplayName("Should propagate exception and retain offset when acknowledgment fails")
        public void consume_whenAcknowledgmentFails_propagatesException() {
                // Arrange
                OrderEvent event = createOrderEvent(Side.BUY, OrderType.LIMIT);
                ConsumerRecord<String, OrderEvent> record = new ConsumerRecord<>(
                                DEFAULT_TOPIC,
                                DEFAULT_PARTITION,
                                DEFAULT_OFFSET,
                                DEFAULT_SYMBOL,
                                event);

                doThrow(new RuntimeException("Kafka commit failed"))
                                .when(acknowledgment).acknowledge();

                // Act & Assert
                assertThrows(
                                RuntimeException.class,
                                () -> orderConsumer.consume(record, acknowledgment));
                verify(acknowledgment).acknowledge();
                verifyNoMoreInteractions(acknowledgment);
        }

        private OrderEvent createOrderEvent(Side side, OrderType orderType) {
                return new OrderEvent(
                                UUID.randomUUID(),
                                DEFAULT_USER_ID,
                                DEFAULT_SYMBOL,
                                side,
                                orderType,
                                DEFAULT_QUANTITY,
                                DEFAULT_PRICE,
                                OrderStatus.PENDING,
                                System.nanoTime());
        }
}
