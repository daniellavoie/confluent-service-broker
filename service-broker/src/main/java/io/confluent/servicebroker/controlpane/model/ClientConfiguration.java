package io.confluent.servicebroker.controlpane.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientConfiguration {
	private Map<String, Object> clientProperties;

	public ClientConfiguration() {

	}

	@JsonCreator
	public ClientConfiguration(@JsonProperty("clientProperties") Map<String, Object> clientProperties) {
		this.clientProperties = clientProperties;
	}

	public Map<String, Object> getClientProperties() {
		return clientProperties;
	}

	public void setClientProperties(Map<String, Object> clientProperties) {
		this.clientProperties = clientProperties;
	}
}
