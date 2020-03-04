package io.confluent.servicebroker.it;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;

public class TestKafkaCredentialsUtil {
	public static void main(String[] args) {
		Properties properties = new Properties();

		properties.put("request.timeout.ms", 20000);
		properties.put("retry.backoff.ms", 500);
		properties.put("bootstrap.servers", "localhost:9092");
		properties.put("security.protocol", "SASL_PLAINTEXT");
		properties.put("sasl.mechanism", "SCRAM-SHA-512");
		properties.put("sasl.jaas.config", "org.apache.kafka.common.security.scram.ScramLoginModule "
				+ "  required username=\"my-org-my-space-confluent-service-broker-sample-app\"" + "  password=\"cUMinB4NTAyOd9Jbh2FRtqWGFNYi2y\";");

		System.out.println(AdminClient.create(properties).listTopics().toString());

	}
}
