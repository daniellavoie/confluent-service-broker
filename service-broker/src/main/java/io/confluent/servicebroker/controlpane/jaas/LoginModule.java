package io.confluent.servicebroker.controlpane.jaas;

public enum LoginModule {
	MD5("org.apache.zookeeper.server.auth.DigestLoginModule"),
	PLAIN("org.apache.kafka.common.security.plain.PlainLoginModule"),
	SCRAM("org.apache.kafka.common.security.scram.ScramLoginModule");

	private final String name;

	LoginModule(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
