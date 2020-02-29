package io.confluent.servicebroker.principal;

import io.confluent.servicebroker.model.CreateKafkaServiceInstanceBindingRequest;
import reactor.core.publisher.Mono;

public interface PrincipalProvider {
	String getSupportedPlatform();
	
	Mono<String> extractPrincipal(CreateKafkaServiceInstanceBindingRequest request);
}
