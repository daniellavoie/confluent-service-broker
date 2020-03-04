package io.confluent.servicebroker.controlpane.provisioning.account;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.ClientConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterCredentials;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

public abstract class AbstractAccountProvisioner implements AccountProvisioner {
	private final Map<String, ClusterProviderConfiguration> clusterProviderConfigurations;

	public AbstractAccountProvisioner(ControlPaneConfiguration controlPaneConfiguration) {
		this.clusterProviderConfigurations = controlPaneConfiguration.getClusterProviders().stream()
				.collect(Collectors.toMap(ClusterProviderConfiguration::getProviderName,
						clusterProviderConfiguration -> clusterProviderConfiguration));
	}

	protected ClusterProviderConfiguration getClusterProviderConfiguration(String clusterProviderName) {
		return Optional.ofNullable(clusterProviderConfigurations.get(clusterProviderName)).orElseThrow(
				() -> new IllegalArgumentException("No cluster provider exists for " + clusterProviderName));
	}

	protected abstract ClusterCredentials getCredentials(String principal,
			ClusterProviderConfiguration clusterProviderConfiguration);

	@Override
	public Map<String, Object> provisionCredentials(String credentialsName, String clusterProviderName,
			ClusterConfiguration clusterConfiguration, ClientConfiguration clientConfiguration) {
		Map<String, Object> credentials = new HashMap<>();

		ClusterProviderConfiguration clusterProviderConfiguration = getClusterProviderConfiguration(
				clusterProviderName);

		ClusterCredentials clusterCredentials = getCredentials(credentialsName, clusterProviderConfiguration);

		credentials.putAll(clusterProviderConfiguration.getClientProperties());
		credentials.putAll(clusterConfiguration.getClientProperties());

		if (clientConfiguration != null && clientConfiguration.getClientProperties() != null) {
			credentials.putAll(clientConfiguration.getClientProperties());
		}

		if (clusterProviderConfiguration.getLoginModule() != null) {
			credentials.put("sasl.jaas.config",
					clusterProviderConfiguration.getLoginModule().getName() + " required username=\""
							+ clusterCredentials.getUsername() + "\" password=\"" + clusterCredentials.getPassword()
							+ "\";");
		}

		return credentials;
	}

	@Override
	public void deprovisionCredentials(String principal, String clusterProviderName) {
		removeCredentials(principal, getClusterProviderConfiguration(clusterProviderName));
	}

	protected String randomString(int length) {
		int leftLimit = 48; // numeral '0'
		int rightLimit = 122; // letter 'z'
		Random random = new Random();

		return random.ints(leftLimit, rightLimit + 1).filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
				.limit(length).collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
	}

	public abstract void removeCredentials(String principal, ClusterProviderConfiguration clusterProviderConfiguration);
}
