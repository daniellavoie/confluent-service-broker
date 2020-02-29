package io.confluent.servicebroker.controlpane.provisioning.account;

import java.util.Map;

import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClientConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;

public interface AccountProvisioner {
	void assertAccountProviderConfiguration(AccountProviderConfiguration accountProviderConfiguration);

	void deprovisionCredentials(String principal, String clusterProviderName);

	Map<String, Object> provisionCredentials(String principal, String clusterProviderName,
			ClusterConfiguration clusterConfiguration, ClientConfiguration clientConfiguration);

	ProvisionerType getProvisionerType();
}