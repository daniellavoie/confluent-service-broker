package io.confluent.servicebroker.cf.config;

import java.util.Optional;

import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.confluent.servicebroker.cf.CloudFoundryService;
import io.confluent.servicebroker.cf.DefaultCloudFoundryService;
import io.confluent.servicebroker.cf.MockCloudFoundryService;

@Configuration
public class CloudFoundryConfig {

	@Bean
	public CloudFoundryService cloudFoundryService(CloudFoundryConfiguration cloudFoundryConfiguration) {
		return Optional.ofNullable(cloudFoundryConfiguration.getApiHost())

				.<CloudFoundryService>map(apiHost -> new DefaultCloudFoundryService(ReactorCloudFoundryClient.builder()
						.connectionContext(DefaultConnectionContext.builder()

								.apiHost(apiHost)

								.build())
						.tokenProvider(PasswordGrantTokenProvider.builder()

								.password(cloudFoundryConfiguration.getPassword())

								.username(cloudFoundryConfiguration.getUsername())

								.build())
						.build()))
				.orElseGet(() -> new MockCloudFoundryService());
	}
}
