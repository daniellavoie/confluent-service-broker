package io.confluent.servicebroker.cf;

import reactor.core.publisher.Mono;

public class MockCloudFoundryService implements CloudFoundryService {

	@Override
	public Mono<String> getApplicationName(String guid) {
		return Mono.just("a-mocked-application");
	}

}
