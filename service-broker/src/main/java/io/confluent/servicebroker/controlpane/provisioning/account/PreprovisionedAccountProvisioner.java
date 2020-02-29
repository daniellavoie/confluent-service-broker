package io.confluent.servicebroker.controlpane.provisioning.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClientConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterCredentials;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

@Component
public class PreprovisionedAccountProvisioner implements AccountProvisioner {
	private final Map<String, ClusterProviderConfiguration> clusterProviderConfigurations;

	public PreprovisionedAccountProvisioner(ControlPaneConfiguration controlPaneConfiguration) {
		this.clusterProviderConfigurations = controlPaneConfiguration.getClusterProviders().stream()
				.collect(Collectors.toMap(ClusterProviderConfiguration::getProviderName,
						clusterProviderConfiguration -> clusterProviderConfiguration));
	}

	@Override
	public void assertAccountProviderConfiguration(AccountProviderConfiguration accountProviderConfiguration) {
		// Do Nothing.
	}

	@Override
	public void deprovisionCredentials(String principal, String clusterProviderName) {
		// Nothing to do.
	}

	private ClusterProviderConfiguration getClusterProviderConfiguration(String clusterProviderName) {
		return Optional.ofNullable(clusterProviderConfigurations.get(clusterProviderName)).orElseThrow(
				() -> new IllegalArgumentException("No cluster provider exists for " + clusterProviderName));
	}

	@Override
	public Map<String, Object> provisionCredentials(String credentialsName, String clusterProviderName,
			ClusterConfiguration clusterConfiguration, ClientConfiguration clientConfiguration) {
		Map<String, Object> credentials = new HashMap<>();

		ClusterProviderConfiguration clusterProviderConfiguration = getClusterProviderConfiguration(
				clusterProviderName);

		ClusterCredentials clusterCredentials = clusterProviderConfiguration.getCredentials().stream()
				.filter(configuredCredentials -> credentialsName.equals(configuredCredentials.getName())).findAny()
				.orElseThrow(() -> new IllegalArgumentException(
						"Could not find any credentials for " + credentialsName + "."));

		credentials.putAll(clusterProviderConfiguration.getClientProperties());
		credentials.putAll(clusterConfiguration.getClientProperties());

		if (clientConfiguration != null && clientConfiguration.getClientProperties() != null) {
			credentials.putAll(clientConfiguration.getClientProperties());
		}

		credentials.put("sasl.jaas.config",
				"org.apache.kafka.common.security.plain.PlainLoginModule required username=\""
						+ clusterCredentials.getUsername() + "\" password=\"" + clusterCredentials.getPassword()
						+ "\";");

		return credentials;
	}

	@Override
	public ProvisionerType getProvisionerType() {
		return ProvisionerType.PREPROVISIONED;
	}
}
