package com.orderlifecycle.ingress_service.PublisherTests;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.orderlifecycle.ingress_service.publisher.KafkaOrderTopic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("KafkaOrderTopic Configuration Tests")
public class KafkaOrderTopicTest {

    private static final String EXPECTED_TOPIC_NAME = "orders";
    private static final int EXPECTED_PARTITIONS = 10;
    private static final short EXPECTED_REPLICAS = 1;

    private final KafkaOrderTopic kafkaOrderTopic = new KafkaOrderTopic();

    @Test
    @DisplayName("Should create orders topic with " + EXPECTED_PARTITIONS + " partitions and replication factor of "
            + EXPECTED_REPLICAS)
    public void topic_createsTopicWithExpectedConfiguration() {
        // Act
        NewTopic topic = kafkaOrderTopic.topic();

        // Assert
        assertNotNull(topic, "NewTopic bean must not be null");
        assertEquals(EXPECTED_TOPIC_NAME, topic.name(), "Topic name must match configuration");
        assertEquals(EXPECTED_PARTITIONS, topic.numPartitions(), "Partitions count must match configuration");
        assertEquals(EXPECTED_REPLICAS, topic.replicationFactor(), "Replication factor must match configuration");
    }
}
