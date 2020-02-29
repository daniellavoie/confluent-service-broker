package io.confluent.servicebroker.controlpane.model;

public class KafkaEndpoint {
	private final String host;
	private final String port;

	public KafkaEndpoint(String host, String port) {
		this.host = host;
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public String getPort() {
		return port;
	}
}
