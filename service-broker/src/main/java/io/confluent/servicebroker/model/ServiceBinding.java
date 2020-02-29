package io.confluent.servicebroker.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.confluent.servicebroker.controlpane.model.KafkaEndpoint;

public class ServiceBinding {
	public enum State {
		CREATING, DELETING, CREATED, ERROR
	}

	private final String bindingId;
	private final String principal;
	private final List<KafkaEndpoint> endpoints;
	private final Map<String, Object> credentials;
	private final Map<String, Object> parameters;
	private final State state;

	@JsonCreator
	public ServiceBinding(@JsonProperty("bindingId") String bindingId, @JsonProperty("principal") String principal,
			@JsonProperty("endpoints") List<KafkaEndpoint> endpoints,
			@JsonProperty("credentials") Map<String, Object> credentials,
			@JsonProperty("parameters") Map<String, Object> parameters, @JsonProperty("state") State state) {
		this.bindingId = bindingId;
		this.principal = principal;
		this.endpoints = endpoints;
		this.credentials = credentials;
		this.parameters = parameters;
		this.state = state;
	}

	public String getBindingId() {
		return bindingId;
	}

	public String getPrincipal() {
		return principal;
	}

	public List<KafkaEndpoint> getEndpoints() {
		return endpoints;
	}

	public Map<String, Object> getCredentials() {
		return credentials;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public State getState() {
		return state;
	}
}
