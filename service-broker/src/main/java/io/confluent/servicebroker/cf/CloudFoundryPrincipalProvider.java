package io.confluent.servicebroker.cf;

import java.util.Optional;

import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.stereotype.Component;

import io.confluent.servicebroker.model.CreateKafkaServiceInstanceBindingRequest;
import io.confluent.servicebroker.principal.PrincipalProvider;
import reactor.core.publisher.Mono;

@Component
public class CloudFoundryPrincipalProvider implements PrincipalProvider {
	private CloudFoundryService cloudFoundryService;

	public CloudFoundryPrincipalProvider(CloudFoundryService cloudFoundryService) {
		this.cloudFoundryService = cloudFoundryService;
	}

	@Override
	public String getSupportedPlatform() {
		return "cloudfoundry";
	}

	@Override
	public Mono<String> extractPrincipal(CreateKafkaServiceInstanceBindingRequest request) {
		String orgName = Optional
				.ofNullable(request.getContext().getProperty(CloudFoundryContext.ORGANIZATION_NAME_KEY))
				.map(Object::toString).orElseThrow(() -> new IllegalArgumentException("Expected "
						+ CloudFoundryContext.ORGANIZATION_NAME_KEY + " is the Cloud Foundry binding context."));

		String spaceName = Optional.ofNullable(request.getContext().getProperty(CloudFoundryContext.SPACE_NAME_KEY))
				.map(Object::toString).orElseThrow(() -> new IllegalArgumentException(
						"Expected " + CloudFoundryContext.SPACE_NAME_KEY + " is the Cloud Foundry binding context."));

		return cloudFoundryService.getApplicationName(request.getBindResource().getAppGuid())

				.map(applicationName -> orgName + "/" + spaceName + "/" + applicationName)

				.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(
						"Could not find application " + request.getBindResource().getAppGuid())));
	}

}
