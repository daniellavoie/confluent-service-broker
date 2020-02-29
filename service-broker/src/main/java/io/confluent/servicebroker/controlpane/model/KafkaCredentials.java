package io.confluent.servicebroker.controlpane.model;

public class KafkaCredentials {
	private final String apiSecret;
	private final String apiKey;

	public KafkaCredentials(String apiSecret, String apiKey) {
		this.apiSecret = apiSecret;
		this.apiKey = apiKey;
	}

	public String getApiSecret() {
		return apiSecret;
	}

	public String getApiKey() {
		return apiKey;
	}
}
