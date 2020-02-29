package io.confluent.servicebroker.it.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ClusterConfiguration {
	private Map<String, Object> clientProperties = new HashMap<>();
	private List<Topic> topics = new ArrayList<>();

	public ClusterConfiguration() {

	}

	@JsonCreator
	public ClusterConfiguration(@JsonProperty("clientProperties") Map<String, Object> clientProperties,
			@JsonProperty("topics") List<Topic> topics) {
		this.clientProperties = clientProperties;
		this.topics = topics;
	}

	public Map<String, Object> getClientProperties() {
		return clientProperties;
	}

	public void setClientProperties(Map<String, Object> clientProperties) {
		this.clientProperties = clientProperties;
	}

	public List<Topic> getTopics() {
		return topics;
	}

	public void setTopics(List<Topic> topics) {
		this.topics = topics;
	}
}
