package io.confluent.servicebroker.controlpane.model;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.NestedConfigurationProperty;

import io.confluent.servicebroker.controlpane.jaas.LoginModule;

public class ClusterProviderConfiguration {
	private String providerName;
	private Map<String, String> clientProperties;

	@NestedConfigurationProperty
	private List<ClusterCredentials> credentials;

	@NestedConfigurationProperty
	private ClusterCredentials genericCredentials;

	private LoginModule loginModule;

	@NestedConfigurationProperty
	private ZookeeperConfiguration zookeeper;

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

	public LoginModule getLoginModule() {
		return loginModule;
	}

	public void setLoginModule(LoginModule loginModule) {
		this.loginModule = loginModule;
	}

	public ZookeeperConfiguration getZookeeper() {
		return zookeeper;
	}

	public void setZookeeper(ZookeeperConfiguration zookeeper) {
		this.zookeeper = zookeeper;
	}
}
