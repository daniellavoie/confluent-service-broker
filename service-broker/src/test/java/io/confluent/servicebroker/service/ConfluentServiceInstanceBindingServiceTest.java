package io.confluent.servicebroker.service;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.model.Topic;
import io.confluent.servicebroker.controlpane.model.TopicACL;

public class ConfluentServiceInstanceBindingServiceTest {
	@Test
	public void assertSerialization() throws JsonProcessingException {
		ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

		ClusterConfiguration userConfiguration = new ClusterConfiguration(new HashMap<>(),
				Arrays.asList(new Topic("topic-1", "test-service", new HashMap<>(),
						Arrays.asList(new TopicACL("topic-1", "test-service", true, true, true)))));

		GetServiceInstanceResponse response = GetServiceInstanceResponse.builder()
				.parameters("topics", userConfiguration.getTopics())
				.parameters("clientProperties", userConfiguration.getClientProperties()).build();

		String expectedJson = "{\"parameters\":{\"topics\":[{\"name\":\"topic-1\",\"owner\":\"test-service\","
				+ "\"configurations\":{},\"acls\":[{\"topic\":\"topic-1\",\"principal\":\"test-service\","
				+ "\"consumer\":true,\"producer\":true,\"idempotent\":true}]}],\"clientProperties\":{}}}";

		Assertions.assertEquals(expectedJson, objectMapper.writeValueAsString(response));
	}
}
