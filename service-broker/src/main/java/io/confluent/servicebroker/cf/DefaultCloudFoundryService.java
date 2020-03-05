package io.confluent.servicebroker.cf;

import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.client.v3.applications.GetApplicationRequest;

import reactor.core.publisher.Mono;

public class DefaultCloudFoundryService implements CloudFoundryService {
	private CloudFoundryClient cloudFoundryClient;

	public DefaultCloudFoundryService(CloudFoundryClient cloudFoundryClient) {
		this.cloudFoundryClient = cloudFoundryClient;
	}

	@Override
	public Mono<String> getApplicationName(String guid) {
		return cloudFoundryClient.applicationsV3().get(GetApplicationRequest.builder().applicationId(guid).build())

				.map(response -> response.getName());
	}
}
