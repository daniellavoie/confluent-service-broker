package io.confluent.servicebroker.it.model;

public class TopicACL {
	public enum Operation {
		Read,
	}

	private final String topic;
	private final String principal;
	private final boolean consumer;
	private final boolean producer;
	private final boolean idempotent;

	public TopicACL(String topic, String principal, boolean consumer, boolean producer, boolean idempotent) {
		this.topic = topic;
		this.principal = principal;
		this.consumer = consumer;
		this.producer = producer;
		this.idempotent = idempotent;
	}

	public String getTopic() {
		return topic;
	}

	public String getPrincipal() {
		return principal;
	}

	public boolean isConsumer() {
		return consumer;
	}

	public boolean isProducer() {
		return producer;
	}

	public boolean isIdempotent() {
		return idempotent;
	}
}
