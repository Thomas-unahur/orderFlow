package com.core.engine.consumer;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.core.engine.dto.OrderEvent;
import com.core.engine.dto.enums.OrderStatus;
import com.core.engine.dto.enums.OrderType;
import com.core.engine.dto.enums.Side;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@EmbeddedKafka(partitions = 1, topics = { "${app.kafka.topics.orders}" })
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("OrderConsumer Embedded Kafka Integration Tests")
public class OrderConsumerIntegrationTest {

    private static final String DEFAULT_SYMBOL = "AAPL";
    private static final String DEFAULT_USER_ID = "usr-int-456";
    private static final BigDecimal DEFAULT_QUANTITY = new BigDecimal("15.50");
    private static final BigDecimal DEFAULT_PRICE = new BigDecimal("220.75");

    @Value("${app.kafka.topics.orders}")
    private String ordersTopic;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @MockitoSpyBean
    private OrderConsumer orderConsumer;

    private KafkaTemplate<String, String> testKafkaTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        Map<String, Object> configs = new HashMap<>(KafkaTestUtils.producerProps(embeddedKafkaBroker));
        ProducerFactory<String, String> producerFactory = new DefaultKafkaProducerFactory<>(
                configs, new StringSerializer(), new StringSerializer());
        testKafkaTemplate = new KafkaTemplate<>(producerFactory);
        objectMapper = new ObjectMapper();
    }

    @ParameterizedTest(name = "Side: {0}")
    @EnumSource(Side.class)
    @DisplayName("Should receive, deserialize JSON from Kafka topic into OrderEvent DTO and acknowledge offset for all order sides")
    void consume_publishedJsonEventAllSides_deserializesAndProcessesSuccessfully(Side side) throws Exception {
        // Arrange
        OrderEvent expectedEvent = createOrderEvent(side, OrderType.LIMIT);
        String jsonPayload = objectMapper.writeValueAsString(expectedEvent);

        // Act
        testKafkaTemplate.send(ordersTopic, expectedEvent.symbol(), jsonPayload).get(5, TimeUnit.SECONDS);

        // Assert
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            @SuppressWarnings("unchecked")
            ArgumentCaptor<ConsumerRecord<String, OrderEvent>> recordCaptor = ArgumentCaptor
                    .forClass(ConsumerRecord.class);
            verify(orderConsumer, atLeastOnce()).consume(recordCaptor.capture(), any(Acknowledgment.class));

            ConsumerRecord<String, OrderEvent> matchedRecord = recordCaptor.getAllValues().stream()
                    .filter(rec -> rec.value() != null && expectedEvent.orderId().equals(rec.value().orderId()))
                    .findFirst()
                    .orElse(null);

            assertThat(matchedRecord).isNotNull();
            assertThat(matchedRecord.topic()).isEqualTo(ordersTopic);
            assertThat(matchedRecord.key()).isEqualTo(expectedEvent.symbol());

            OrderEvent actualEvent = matchedRecord.value();
            assertThat(actualEvent.orderId()).isEqualTo(expectedEvent.orderId());
            assertThat(actualEvent.userId()).isEqualTo(expectedEvent.userId());
            assertThat(actualEvent.symbol()).isEqualTo(expectedEvent.symbol());
            assertThat(actualEvent.side()).isEqualTo(expectedEvent.side());
            assertThat(actualEvent.type()).isEqualTo(expectedEvent.type());
            assertThat(actualEvent.quantity()).isEqualByComparingTo(expectedEvent.quantity());
            assertThat(actualEvent.price()).isEqualByComparingTo(expectedEvent.price());
            assertThat(actualEvent.status()).isEqualTo(expectedEvent.status());
            assertThat(actualEvent.timestamp()).isEqualTo(expectedEvent.timestamp());
        });
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
