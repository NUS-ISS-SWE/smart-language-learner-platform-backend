package com.nus.iss.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class KafkaConfigTest {

    @Test
    void kafkaAdminUsesConfiguredBootstrapServerAndTimeouts() {
        KafkaConfig kafkaConfig = new KafkaConfig(sllpProperties());

        KafkaAdmin kafkaAdmin = kafkaConfig.kafkaAdmin();

        assertEquals("localhost:29092", kafkaAdmin.getConfigurationProperties().get("bootstrap.servers"));
        assertEquals(1000, kafkaAdmin.getConfigurationProperties().get("retry.backoff.ms"));
        assertEquals(30000, kafkaAdmin.getConfigurationProperties().get("request.timeout.ms"));
        assertEquals(60000, kafkaAdmin.getConfigurationProperties().get("default.api.timeout.ms"));
    }

    @Test
    void topicBeansUseConfiguredTopicNames() {
        KafkaConfig kafkaConfig = new KafkaConfig(sllpProperties());

        NewTopic gradingRequested = kafkaConfig.compositionGradingRequestedTopic();
        NewTopic gradingCompleted = kafkaConfig.compositionGradingCompletedTopic();
        NewTopic generationRequested = kafkaConfig.compositionGenerationRequestedTopic();
        NewTopic generationCompleted = kafkaConfig.compositionGenerationCompletedTopic();

        assertEquals("grading-requested-topic", gradingRequested.name());
        assertEquals("grading-completed-topic", gradingCompleted.name());
        assertEquals("generation-requested-topic", generationRequested.name());
        assertEquals("generation-completed-topic", generationCompleted.name());
    }

    @Test
    void producerAndConsumerFactoriesCanBeCreatedFromConfiguredProperties() {
        KafkaConfig kafkaConfig = new KafkaConfig(sllpProperties());

        ProducerFactory<String, String> producerFactory = kafkaConfig.producerFactory();
        KafkaTemplate<String, String> kafkaTemplate = kafkaConfig.kafkaTemplate();
        ConsumerFactory<String, String> consumerFactory = kafkaConfig.consumerFactory();
        ConcurrentKafkaListenerContainerFactory<String, String> listenerFactory =
                kafkaConfig.kafkaListenerContainerFactory();

        assertNotNull(producerFactory);
        assertNotNull(kafkaTemplate);
        assertNotNull(consumerFactory);
        assertNotNull(listenerFactory);
    }

    private static SllpProperties sllpProperties() {
        SllpProperties properties = new SllpProperties();
        properties.getKafka().setBootstrapServers("localhost:29092");
        properties.getKafka().setGroupId("composition-service");
        properties.getKafka().setAutoOffsetReset("earliest");
        properties.getKafka().setRetryBackoffMs(1000);
        properties.getKafka().setRequestTimeoutMs(30000);
        properties.getKafka().setDefaultApiTimeoutMs(60000);

        SllpProperties.Topics topics = new SllpProperties.Topics();
        topics.setCompositionGradingRequested("grading-requested-topic");
        topics.setCompositionGradingCompleted("grading-completed-topic");
        topics.setCompositionGenerationRequested("generation-requested-topic");
        topics.setCompositionGenerationCompleted("generation-completed-topic");
        properties.getKafka().setTopics(topics);

        return properties;
    }
}
