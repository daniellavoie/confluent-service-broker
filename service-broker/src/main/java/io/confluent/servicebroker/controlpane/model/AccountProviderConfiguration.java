package io.confluent.servicebroker.controlpane.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class AccountProviderConfiguration {
	private Map<String, String> clientProperties;
	
	public AccountProviderConfiguration() {
	
	}

	@JsonCreator
	public AccountProviderConfiguration(@JsonProperty("clientProperties") Map<String, String> clientProperties) {
		this.clientProperties = clientProperties;
	}

	public Map<String, String> getClientProperties() {
		return clientProperties;
	}

	public void setClientProperties(Map<String, String> clientProperties) {
		this.clientProperties = clientProperties;
	}
}
