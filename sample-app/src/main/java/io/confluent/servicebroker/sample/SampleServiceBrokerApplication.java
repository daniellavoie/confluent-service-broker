package io.confluent.servicebroker.sample;

import org.apache.kafka.clients.admin.AdminClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.pivotal.cfenv.core.CfEnv;

@Configuration
@SpringBootApplication
public class SampleServiceBrokerApplication {
	private static final Logger LOGGER = LoggerFactory.getLogger(SampleServiceBrokerApplication.class);

	@Bean
	public AdminClient adminClient() {
		return AdminClient.create(new CfEnv().findServiceByTag("kafka").getCredentials().getMap());
	}

	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(SampleServiceBrokerApplication.class, args);

		AdminClient adminClient = context.getBean(AdminClient.class);

		LOGGER.info("Loading topics from Kafka.");

		LOGGER.info("Topics : {}", adminClient.listTopics());
	}
}
