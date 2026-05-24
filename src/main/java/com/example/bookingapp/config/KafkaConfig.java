package com.example.bookingapp.config;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.config.SaslConfigs;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.security.protocol}")
    private String securityProtocol;

    @Value("${spring.kafka.properties.sasl.mechanism}")
    private String saslMechanism;

    @Value("${kafka.sasl.username}")
    private String saslUsername;

    @Value("${kafka.sasl.password}")
    private String saslPassword;

    @Value("${spring.kafka.ssl.trust-store-type}")
    private String trustStoreType;

    @Value("${spring.kafka.ssl.trust-store-location}")
    private String trustStoreLocation;

    @Value("${spring.kafka.ssl.trust-store-password}")
    private String trustStorePassword;

    @Value("${kafka.consumer.group-id}")
    private String consumerGroupId;

    @Value("${kafka.consumer.trusted-packages}")
    private String trustedPackages;

    @Value("${kafka.topic.appointment}")
    private String appointmentTopicName;

    @Value("${kafka.topic.notification}")
    private String notificationTopicName;

    @Value("${kafka.topic.cloudflows}")
    private String cloudflowsTopicName;

    @Value("${kafka.topic.partitions}")
    private int topicPartitions;

    @Value("${kafka.topic.replicas}")
    private int topicReplicas;

    private Map<String, Object> commonSecurityConfig() {
        Map<String, Object> props = new HashMap<>();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, securityProtocol);
        props.put(SaslConfigs.SASL_MECHANISM, saslMechanism);

        String jaasConfig = String.format(
                "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
                saslUsername, saslPassword);
        props.put(SaslConfigs.SASL_JAAS_CONFIG, jaasConfig);

        props.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "");
        props.put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, trustStoreType);
        
        String resolvedTrustStoreLocation = trustStoreLocation;
        if (resolvedTrustStoreLocation != null && resolvedTrustStoreLocation.startsWith("file:")) {
            resolvedTrustStoreLocation = resolvedTrustStoreLocation.substring(5);
            while (resolvedTrustStoreLocation.startsWith("//")) {
                resolvedTrustStoreLocation = resolvedTrustStoreLocation.substring(1);
            }
        }
        props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, resolvedTrustStoreLocation);
        props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, trustStorePassword);

        return props;
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = commonSecurityConfig();
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = commonSecurityConfig();
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    @Bean
    public NewTopic appointmentTopic() {
        return TopicBuilder.name(appointmentTopicName)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    @Bean
    public NewTopic notificationTopic() {
        return TopicBuilder.name(notificationTopicName)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    @Bean
    public NewTopic cloudflowsTopic() {
        return TopicBuilder.name(cloudflowsTopicName)
                .partitions(topicPartitions)
                .replicas(topicReplicas)
                .build();
    }

    // Accessor methods for topic names (used by other services)
    public String getAppointmentTopicName() {
        return appointmentTopicName;
    }

    public String getNotificationTopicName() {
        return notificationTopicName;
    }

    public String getCloudflowsTopicName() {
        return cloudflowsTopicName;
    }
}
