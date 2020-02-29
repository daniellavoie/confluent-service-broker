package io.confluent.servicebroker.cf.config;

import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.DefaultConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CloudFoundryConfig {
	@Bean
	public DefaultConnectionContext connectionContext(CloudFoundryConfiguration cloudFoundryConfiguration) {
		return DefaultConnectionContext.builder().apiHost(cloudFoundryConfiguration.getApiHost()).build();
	}

	@Bean
	public PasswordGrantTokenProvider tokenProvider(CloudFoundryConfiguration cloudFoundryConfiguration) {
		return PasswordGrantTokenProvider.builder().password(cloudFoundryConfiguration.getPassword())
				.username(cloudFoundryConfiguration.getUsername()).build();
	}

	@Bean
	public ReactorCloudFoundryClient cloudFoundryClient(ConnectionContext connectionContext,
			TokenProvider tokenProvider) {
		return ReactorCloudFoundryClient.builder().connectionContext(connectionContext).tokenProvider(tokenProvider)
				.build();
	}
}
