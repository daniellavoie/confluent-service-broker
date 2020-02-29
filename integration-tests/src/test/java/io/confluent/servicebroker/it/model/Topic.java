package io.confluent.servicebroker.it.model;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class Topic {
	private final String name;
	private final String owner;
	private final Map<String, String> configurations;
	private final List<TopicACL> acls;

	@JsonCreator
	public Topic(@JsonProperty("name") String name, @JsonProperty("owner") String owner,
			@JsonProperty("configurations") Map<String, String> configurations,
			@JsonProperty("acls") List<TopicACL> acls) {
		this.name = name;
		this.owner = owner;
		this.configurations = configurations;
		this.acls = acls;
	}

	public String getName() {
		return name;
	}

	public String getOwner() {
		return owner;
	}

	public Map<String, String> getConfigurations() {
		return configurations;
	}

	public List<TopicACL> getAcls() {
		return acls;
	}
}
