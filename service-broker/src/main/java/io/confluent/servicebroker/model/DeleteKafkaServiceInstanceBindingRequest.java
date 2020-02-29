package io.confluent.servicebroker.model;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DeleteKafkaServiceInstanceBindingRequest {
	private final String serviceInstanceId;
	private final String bindingId;
	private final ServiceDefinition serviceDefinition;
	private final Plan plan;
	private final boolean asyncAccepted;

	@JsonCreator
	public DeleteKafkaServiceInstanceBindingRequest(@JsonProperty("serviceInstanceId") String serviceInstanceId,
			@JsonProperty("bindingId") String bindingId,
			@JsonProperty("serviceDefinition") ServiceDefinition serviceDefinition, @JsonProperty("plan") Plan plan,
			@JsonProperty("asyncAccepted") boolean asyncAccepted) {
		this.serviceInstanceId = serviceInstanceId;
		this.bindingId = bindingId;
		this.serviceDefinition = serviceDefinition;
		this.plan = plan;
		this.asyncAccepted = asyncAccepted;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public String getBindingId() {
		return bindingId;
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
