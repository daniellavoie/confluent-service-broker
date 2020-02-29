package io.confluent.servicebroker.controlpane.provisioning.cluster;

import java.util.List;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.KafkaEndpoint;

public class ClusterProvisionResult {
	private final ClusterConfiguration clusterConfiguration;
	private final List<KafkaEndpoint> endpoints;

	public ClusterProvisionResult(ClusterConfiguration clusterConfiguration, List<KafkaEndpoint> endpoints) {
		this.clusterConfiguration = clusterConfiguration;
		this.endpoints = endpoints;
	}

	public ClusterConfiguration getClusterConfiguration() {
		return clusterConfiguration;
	}

	public List<KafkaEndpoint> getEndpoints() {
		return endpoints;
	}
}
