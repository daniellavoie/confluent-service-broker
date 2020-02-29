package io.confluent.servicebroker.controlpane.model;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

public class ClusterProviderConfiguration {
	private String providerName;
	private Map<String, String> clientProperties;

	@NestedConfigurationProperty
	private List<ClusterCredentials> credentials;

	@NestedConfigurationProperty
	private ClusterCredentials genericCredentials;

	public String getProviderName() {
		return providerName;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Map<String, String> getClientProperties() {
		return clientProperties;
	}

	public void setClientProperties(Map<String, String> clientProperties) {
		this.clientProperties = clientProperties;
	}

	public List<ClusterCredentials> getCredentials() {
		return credentials;
	}

	public void setCredentials(List<ClusterCredentials> credentials) {
		this.credentials = credentials;
	}

	public ClusterCredentials getGenericCredentials() {
		return genericCredentials;
	}

	public void setGenericCredentials(ClusterCredentials genericCredentials) {
		this.genericCredentials = genericCredentials;
	}
}
