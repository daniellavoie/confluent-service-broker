package io.confluent.servicebroker.controlpane.provisioning.cluster;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.KafkaEndpoint;

@Component
public class PreprovisionedProvisioner implements ClusterProvisioner {
	private Map<String, ClusterProviderConfiguration> clusterProviderConfigurations;

	public PreprovisionedProvisioner(ControlPaneConfiguration controlPaneConfiguration) {
		controlPaneConfiguration.getClusterProviders().stream().forEach(this::assertClusterProviderConfiguration);

		this.clusterProviderConfigurations = controlPaneConfiguration.getClusterProviders().stream()
				.collect(Collectors.toMap(ClusterProviderConfiguration::getProviderName,
						clusterProviderConfiguration -> clusterProviderConfiguration));

	}

	private void assertClusterProviderConfiguration(ClusterProviderConfiguration clusterProviderConfiguration) {
		assertProperty(clusterProviderConfiguration, "ssl.endpoint.identification.algorithm");
		assertProperty(clusterProviderConfiguration, "sasl.mechanism");
		assertProperty(clusterProviderConfiguration, "request.timeout.ms");
		assertProperty(clusterProviderConfiguration, "bootstrap.servers");
		assertProperty(clusterProviderConfiguration, "retry.backoff.ms");
		assertProperty(clusterProviderConfiguration, "security.protocol");
	}

	private void assertProperty(ClusterProviderConfiguration clusterProviderConfiguration, String property) {
		Optional.ofNullable(clusterProviderConfiguration.getClientProperties().get(property))
				.orElseThrow(() -> new IllegalArgumentException("Could not find property " + property + " for cluster "
						+ clusterProviderConfiguration.getProviderName()));
	}

	@Override
	public void assertUserConfigurations(ClusterConfiguration clusterConfiguration) {
		// Nothing to validate.
	}

	private ClusterProviderConfiguration getClusterProviderConfiguration(String clusterProviderName) {
		return Optional.ofNullable(clusterProviderConfigurations.get(clusterProviderName)).orElseThrow(
				() -> new IllegalArgumentException("No cluster provider exists for " + clusterProviderName));
	}

	@Override
	public ProvisionerType getProvisionerType() {
		return ProvisionerType.PREPROVISIONED;
	}

	private KafkaEndpoint mapToEndpoint(String[] entry) {
		if (entry.length != 2) {
			throw new IllegalArgumentException(Arrays.stream(entry).collect(Collectors.joining(":"))
					+ " is not a valid kafka endpoint configuration");
		}

		return new KafkaEndpoint(entry[0], entry[1]);
	}

	private ClusterConfiguration mergeConfiguration(ClusterConfiguration userConfiguration,
			String clusterProviderName) {
		ClusterProviderConfiguration clusterProviderConfiguration = getClusterProviderConfiguration(
				clusterProviderName);
		Map<String, Object> clientProperties = new HashMap<>();
		clientProperties.putAll(clusterProviderConfiguration.getClientProperties());
		clientProperties.putAll(userConfiguration.getClientProperties());

		return new ClusterConfiguration(clientProperties, userConfiguration.getTopics());
	}
	
	@Override
	public void deprovision(String clusterProviderName, ClusterConfiguration userConfiguration) {
		// TODO Remove ACLs and topics
	}

	@Override
	public ClusterProvisionResult provision(String clusterProviderName, ClusterConfiguration userConfiguration) {
		return updateCluster(clusterProviderName, userConfiguration);
	}

	private void synchronizeACLs(ClusterConfiguration userConfiguration) {
		// TODO Manage ACL on topics.
	}

	@Override
	public ClusterProvisionResult update(String clusterProviderName, ClusterConfiguration userConfiguration) {
		return updateCluster(clusterProviderName, userConfiguration);
	}

	private ClusterProvisionResult updateCluster(String clusterProviderName, ClusterConfiguration userConfiguration) {
		ClusterConfiguration mergedClusterConfiguration = mergeConfiguration(userConfiguration, clusterProviderName);

		synchronizeACLs(userConfiguration);

		List<KafkaEndpoint> endpoints = Arrays
				.stream(mergedClusterConfiguration.getClientProperties().get("bootstrap.servers").toString().split(","))
				.map(entry -> entry.split(":")).map(this::mapToEndpoint).collect(Collectors.toList());

		return new ClusterProvisionResult(mergedClusterConfiguration, endpoints);
	}
}
