package com.nus.iss.config;

import com.nus.iss.contracts.grading.CompositionGradingCompletedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;

import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfiguration {
    @Bean
    ConsumerFactory<String, CompositionGradingCompletedEvent> learningConsumerFactory(KafkaConfig properties) {
        return new DefaultKafkaConsumerFactory<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getBootstrapServers(),
                        ConsumerConfig.GROUP_ID_CONFIG, properties.getGroupId(),
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"),
                new StringDeserializer(),
                new ErrorHandlingDeserializer<>(learningKafkaValueDeserializer()));
    }

    JacksonJsonDeserializer<CompositionGradingCompletedEvent> learningKafkaValueDeserializer() {
        JacksonJsonDeserializer<CompositionGradingCompletedEvent> valueDeserializer =
                new JacksonJsonDeserializer<>(CompositionGradingCompletedEvent.class);
        valueDeserializer.addTrustedPackages("com.nus.iss.contracts.grading");
        return valueDeserializer;
    }

    @Bean
    ConcurrentKafkaListenerContainerFactory<String, CompositionGradingCompletedEvent> learningKafkaListenerContainerFactory(ConsumerFactory<String, CompositionGradingCompletedEvent> learningConsumerFactory) {
        ConcurrentKafkaListenerContainerFactory<String, CompositionGradingCompletedEvent> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(learningConsumerFactory);
        return factory;
    }
}
