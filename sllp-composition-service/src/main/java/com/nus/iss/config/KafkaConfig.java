package com.nus.iss.config;

import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConfig {

    private final SllpProperties sllpProperties;

    public KafkaConfig(SllpProperties sllpProperties) {
        this.sllpProperties = sllpProperties;
    }

    @Bean
    public KafkaAdmin kafkaAdmin() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, sllpProperties.getKafka().getBootstrapServers());
        // Use properties for timeouts
        configs.put(AdminClientConfig.RETRY_BACKOFF_MS_CONFIG, sllpProperties.getKafka().getRetryBackoffMs());
        configs.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, sllpProperties.getKafka().getRequestTimeoutMs());
        configs.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, sllpProperties.getKafka().getDefaultApiTimeoutMs());
        
        KafkaAdmin kafkaAdmin = new KafkaAdmin(configs);
        // This will cause the application context to fail if Kafka is not reachable on startup
        kafkaAdmin.setFatalIfBrokerNotAvailable(true);
        return kafkaAdmin;
    }

    @Bean
    public NewTopic compositionGradingRequestedTopic() {
        return TopicBuilder.name(sllpProperties.getKafka().getTopics().getCompositionGradingRequested())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compositionGradingCompletedTopic() {
        return TopicBuilder.name(sllpProperties.getKafka().getTopics().getCompositionGradingCompleted())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compositionGradingProgressTopic() {
        return TopicBuilder.name(sllpProperties.getKafka().getTopics().getCompositionGradingProgress())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compositionGenerationRequestedTopic() {
        return TopicBuilder.name(sllpProperties.getKafka().getTopics().getCompositionGenerationRequested())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic compositionGenerationCompletedTopic() {
        return TopicBuilder.name(sllpProperties.getKafka().getTopics().getCompositionGenerationCompleted())
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, sllpProperties.getKafka().getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, sllpProperties.getKafka().getBootstrapServers());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, sllpProperties.getKafka().getGroupId());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, sllpProperties.getKafka().getAutoOffsetReset());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.getContainerProperties().setMissingTopicsFatal(true);
        return factory;
    }
}
