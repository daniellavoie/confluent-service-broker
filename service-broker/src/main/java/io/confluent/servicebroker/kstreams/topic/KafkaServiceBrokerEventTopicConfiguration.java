package io.confluent.servicebroker.kstreams.topic;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConstructorBinding
@ConfigurationProperties("topics.kafka-service-broker-event")
public class KafkaServiceBrokerEventTopicConfiguration extends TopicConfiguration {

}
