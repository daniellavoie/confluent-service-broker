package io.confluent.servicebroker.controlpane.provisioning.cluster;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;

public interface ClusterProvisioner {
	void assertUserConfigurations(ClusterConfiguration userConfiguration);

	ProvisionerType getProvisionerType();

	void deprovision(String clusterProviderName, ClusterConfiguration userConfiguration);

	ClusterProvisionResult provision(String clusterProviderName, ClusterConfiguration userConfiguration);

	ClusterProvisionResult update(String clusterProviderName, ClusterConfiguration userConfiguration);
}