package io.confluent.servicebroker.it;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.servicebroker.autoconfigure.web.ServiceBrokerAutoConfiguration;
import org.springframework.cloud.servicebroker.autoconfigure.web.reactive.ServiceBrokerWebFluxAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.reactive.function.client.WebClient;

@SpringBootApplication(exclude = { ServiceBrokerAutoConfiguration.class, ServiceBrokerWebFluxAutoConfiguration.class })
public class ServiceBrokerIntegrationTests {

	@Bean
	public WebClient webClient(@Value("${servicebroker.url}") String serviceBrokerUrl) {
		return WebClient.builder().baseUrl(serviceBrokerUrl).build();
	}
}
