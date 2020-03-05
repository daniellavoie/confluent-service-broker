package io.confluent.servicebroker.cf;

import reactor.core.publisher.Mono;

public interface CloudFoundryService {
	Mono<String> getApplicationName(String guid);
}
