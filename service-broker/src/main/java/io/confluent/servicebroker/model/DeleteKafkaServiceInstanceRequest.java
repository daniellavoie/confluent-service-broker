package io.confluent.servicebroker.model;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteKafkaServiceInstanceRequest {
	private final String serviceInstanceId;
	private final ServiceDefinition serviceDefinition;
	private final Plan plan;
	private final boolean asyncAccepted;

	@JsonCreator
	public DeleteKafkaServiceInstanceRequest(@JsonProperty("serviceInstanceId") String serviceInstanceId,
			@JsonProperty("serviceDefinition") ServiceDefinition serviceDefinition, @JsonProperty("plan") Plan plan,
			@JsonProperty("asyncAccepted") boolean asyncAccepted) {
		this.serviceInstanceId = serviceInstanceId;
		this.serviceDefinition = serviceDefinition;
		this.plan = plan;
		this.asyncAccepted = asyncAccepted;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public ServiceDefinition getServiceDefinition() {
		return serviceDefinition;
	}

	public Plan getPlan() {
		return plan;
	}

	public boolean isAsyncAccepted() {
		return asyncAccepted;
	}
}
