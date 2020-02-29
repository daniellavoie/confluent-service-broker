package io.confluent.servicebroker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.kafka.annotation.EnableKafkaStreams;

@EnableKafkaStreams
@SpringBootApplication
@EnableConfigurationProperties
public class ConfluentServiceBrokerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ConfluentServiceBrokerApplication.class, args);
	}

}
