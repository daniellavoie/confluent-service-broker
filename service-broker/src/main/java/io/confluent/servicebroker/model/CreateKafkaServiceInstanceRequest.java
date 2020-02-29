package io.confluent.servicebroker.model;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ProvisionerType;

public class CreateKafkaServiceInstanceRequest {
	private final String serviceInstanceId;
	private final boolean asyncAccepted;
	private final ServiceDefinition serviceDefinition;
	private final Plan plan;
	private final ClusterConfiguration clusterConfiguration;
	private final ProvisionerType providerType;

	@JsonCreator
	public CreateKafkaServiceInstanceRequest(@JsonProperty("serviceInstanceId") String serviceInstanceId,
			@JsonProperty("asyncAccepted") boolean asyncAccepted,
			@JsonProperty("serviceDefinition") ServiceDefinition serviceDefinition, @JsonProperty("plan") Plan plan,
			@JsonProperty("clusterConfiguration") ClusterConfiguration clusterConfiguration,
			@JsonProperty("providerType") ProvisionerType providerType) {
		this.serviceInstanceId = serviceInstanceId;
		this.asyncAccepted = asyncAccepted;
		this.serviceDefinition = serviceDefinition;
		this.plan = plan;
		this.clusterConfiguration = clusterConfiguration;
		this.providerType = providerType;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public boolean isAsyncAccepted() {
		return asyncAccepted;
	}

	public ServiceDefinition getServiceDefinition() {
		return serviceDefinition;
	}

	public Plan getPlan() {
		return plan;
	}

	public ClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	public ProvisionerType getProviderType() {
		return providerType;
	}
}
