package io.confluent.servicebroker.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.KafkaEndpoint;

public class ServiceInstance {
	public enum State {
		CREATING, DELETING, UPDATING, CREATED, ERROR
	}

	private final String instanceId;
	private final String serviceDefinitionId;
	private final String planId;
	private final List<KafkaEndpoint> endpoints;
	private final ClusterConfiguration clusterConfiguration;
	private final State state;

	@JsonCreator
	public ServiceInstance(@JsonProperty("instanceId") String instanceId,
			@JsonProperty("serviceDefinitionId") String serviceDefinitionId, @JsonProperty("planId") String planId,
			@JsonProperty("endpoints") List<KafkaEndpoint> endpoints,
			@JsonProperty("parameters") ClusterConfiguration clusterConfiguration, @JsonProperty("state") State state) {
		this.instanceId = instanceId;
		this.serviceDefinitionId = serviceDefinitionId;
		this.planId = planId;
		this.endpoints = endpoints;
		this.clusterConfiguration = clusterConfiguration;
		this.state = state;
	}

	public String getInstanceId() {
		return instanceId;
	}

	public String getServiceDefinitionId() {
		return serviceDefinitionId;
	}

	public String getPlanId() {
		return planId;
	}

	public List<KafkaEndpoint> getEndpoints() {
		return endpoints;
	}

	public ClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	public State getState() {
		return state;
	}

}
