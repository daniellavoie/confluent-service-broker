package io.confluent.servicebroker.controlpane.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import org.springframework.context.annotation.Configuration;

import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

@Configuration
@ConfigurationProperties(prefix = "confluent.servicebroker.controlpane")
public class ControlPaneConfiguration {
	@NestedConfigurationProperty
	private List<ClusterProviderConfiguration> clusterProviders;

	public List<ClusterProviderConfiguration> getClusterProviders() {
		return clusterProviders;
	}

	public void setClusterProviders(List<ClusterProviderConfiguration> clusterProviders) {
		this.clusterProviders = clusterProviders;
	}
}
