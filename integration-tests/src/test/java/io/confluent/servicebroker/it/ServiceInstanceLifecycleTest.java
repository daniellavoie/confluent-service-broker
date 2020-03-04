package io.confluent.servicebroker.it;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.servicebroker.model.CloudFoundryContext;
import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.catalog.Catalog;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.web.reactive.function.client.WebClient;

import io.confluent.servicebroker.it.model.ClusterConfiguration;
import io.confluent.servicebroker.it.model.Topic;
import io.confluent.servicebroker.it.model.TopicACL;

@SpringBootTest(classes = ServiceBrokerIntegrationTests.class)
public abstract class ServiceInstanceLifecycleTest {
	@Autowired
	private WebClient webClient;

	@Value("${app.guid}")
	private String appGuid;

	private GetLastServiceBindingOperationResponse pollLastBindingOperation(String serviceInstanceId, String bindingId,
			OperationState currentOperation) {
		try {
			int maxAttempts = 5;
			int sleepMillis = 2000;

			GetLastServiceBindingOperationResponse lastOperationResponse;
			for (int attempt = 0; attempt <= maxAttempts; attempt++) {
				if (attempt != 0) {
					Thread.sleep(sleepMillis);
				}

				lastOperationResponse = webClient.get()
						.uri("/v2/service_instances/" + serviceInstanceId + "/service_bindings/" + bindingId
								+ "/last_operation")
						.exchange()
						.flatMap(response -> response.bodyToMono(GetLastServiceBindingOperationResponse.class)).block();

				if (lastOperationResponse != null && !currentOperation.equals(lastOperationResponse.getState())) {
					return lastOperationResponse;
				}
			}
			throw new RuntimeException("Service instance operation state is still " + currentOperation.getValue());
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	private GetLastServiceOperationResponse pollLastServiceOperation(String serviceInstanceId,
			OperationState currentOperation) {
		try {
			int maxAttempts = 5;
			int sleepMillis = 2000;

			GetLastServiceOperationResponse lastOperationResponse;
			for (int attempt = 0; attempt <= maxAttempts; attempt++) {
				if (attempt != 0) {
					Thread.sleep(sleepMillis);
				}

				lastOperationResponse = webClient.get()
						.uri("/v2/service_instances/" + serviceInstanceId + "/last_operation").exchange()
						.flatMap(response -> response.bodyToMono(GetLastServiceOperationResponse.class)).block();

				if (lastOperationResponse != null && !currentOperation.equals(lastOperationResponse.getState())) {
					return lastOperationResponse;
				}
			}
			throw new RuntimeException("Service instance operation state is still " + currentOperation.getValue());
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

	protected abstract Plan getPlan(ServiceDefinition serviceDefinition);

	@Test
	public void assertServiceInstanceLifecycle() throws InterruptedException, ExecutionException {
		Catalog catalog = webClient.get().uri("/v2/catalog").exchange()
				.flatMap(response -> response.bodyToMono(Catalog.class)).block();

		Assertions.assertEquals(1, catalog.getServiceDefinitions().size());

		ServiceDefinition serviceDefinition = catalog.getServiceDefinitions().get(0);

		Plan plan = getPlan(serviceDefinition);

		ClusterConfiguration userConfiguration = new ClusterConfiguration(new HashMap<>(),
				Arrays.asList(new Topic("topic-1", "test-service", new HashMap<>(),
						Arrays.asList(new TopicACL("topic-1", "test-service", true, true, true)))));

		String serviceInstanceId = UUID.randomUUID().toString();

		Context context = CloudFoundryContext.builder()

				.platform("cloudfoundry")

				.organizationGuid("org-guid-here")

				.organizationName("my-org")

				.spaceName("my-space")

				.spaceGuid("space-guid-here")

				.build();

		CreateServiceInstanceRequest createServiceInstance = CreateServiceInstanceRequest.builder()

				.context(context)

				.serviceDefinitionId(serviceDefinition.getId())

				.parameters("topic", userConfiguration.getTopics())

				.parameters("clientProperties", userConfiguration.getClientProperties())

				.planId(plan.getId())

				.asyncAccepted(true)

				.build();

		CreateServiceInstanceResponse createServiceInstanceResponse = webClient.put()
				.uri("/v2/service_instances/" + serviceInstanceId + "?accepts_incomplete=true")
				.bodyValue(createServiceInstance).exchange()
				.flatMap(response -> response.bodyToMono(CreateServiceInstanceResponse.class))
				.block(Duration.ofSeconds(5));

		Assertions.assertEquals(OperationState.IN_PROGRESS.toString(), createServiceInstanceResponse.getOperation());

		Assertions.assertEquals(OperationState.SUCCEEDED,
				pollLastServiceOperation(serviceInstanceId, OperationState.IN_PROGRESS).getState());

		// Bind application.
		CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest = CreateServiceInstanceBindingRequest
				.builder()

				.context(context)

				.bindResource(BindResource.builder().appGuid(appGuid).build())

				.serviceInstanceId(serviceInstanceId)

				.serviceDefinitionId(serviceDefinition.getId())

				.planId(plan.getId())

				.build();

		String bindingId = UUID.randomUUID().toString();

		CreateServiceInstanceAppBindingResponse createServiceInstanceBindingResponse = webClient.put()
				.uri("/v2/service_instances/" + serviceInstanceId + "/service_bindings/" + bindingId
						+ "?accepts_incomplete=true")

				.bodyValue(createServiceInstanceBindingRequest)

				.exchange()

				.flatMap(response -> response.bodyToMono(CreateServiceInstanceAppBindingResponse.class))

				.block(Duration.ofSeconds(5));

		Assertions.assertEquals(OperationState.IN_PROGRESS.toString(),
				createServiceInstanceBindingResponse.getOperation());

		Assertions.assertEquals(OperationState.SUCCEEDED,
				pollLastBindingOperation(serviceInstanceId, bindingId, OperationState.IN_PROGRESS).getState());

		GetServiceInstanceAppBindingResponse getServiceInstanceAppBindingResponse = webClient.get()

				.uri("/v2/service_instances/" + serviceInstanceId + "/service_bindings/" + bindingId)

				.exchange()

				.flatMap(response -> response.bodyToMono(GetServiceInstanceAppBindingResponse.class))

				.block(Duration.ofSeconds(5));

		KafkaProperties kafkaProperties = new KafkaProperties();
		kafkaProperties.getProperties().putAll(getServiceInstanceAppBindingResponse.getCredentials().entrySet().stream()
				.collect(Collectors.toMap(Entry::getKey, entry -> entry.getValue().toString())));

		AdminClient adminClient = AdminClient.create(kafkaProperties.buildAdminProperties());

		Assertions.assertNotNull(adminClient.listTopics().names().get());

		DeleteServiceInstanceBindingResponse deleteServiceInstanceBindingResponse = webClient.delete()

				.uri("/v2/service_instances/" + serviceInstanceId + "/service_bindings/" + bindingId
						+ "?accepts_incomplete=true&service_id=" + serviceDefinition.getId() + "&plan_id="
						+ plan.getId())

				.exchange()

				.flatMap(response -> response.bodyToMono(DeleteServiceInstanceBindingResponse.class))

				.block(Duration.ofSeconds(5));

		Assertions.assertEquals(OperationState.IN_PROGRESS.toString(),
				deleteServiceInstanceBindingResponse.getOperation());

		DeleteServiceInstanceResponse deleteServiceInstanceResponse = webClient.delete()

				.uri("/v2/service_instances/" + serviceInstanceId + "?accepts_incomplete=true&service_id="
						+ serviceDefinition.getId() + "&plan_id=" + plan.getId())

				.exchange().flatMap(response -> response.bodyToMono(DeleteServiceInstanceResponse.class))

				.block(Duration.ofSeconds(5));

		Assertions.assertEquals(OperationState.IN_PROGRESS.toString(), deleteServiceInstanceResponse.getOperation());

		int attempt = 0;
		GetServiceInstanceResponse getServiceInstanceResponse;
		do {
			getServiceInstanceResponse = webClient.get().uri("/v2/service_instances/" + serviceInstanceId)

					.exchange().flatMap(response -> response.bodyToMono(GetServiceInstanceResponse.class))

					.block(Duration.ofSeconds(5));
			if (getServiceInstanceResponse == null) {
				return;
			}

			Thread.sleep(1000);
			++attempt;
		} while (attempt < 5);

		if (getServiceInstanceResponse != null) {
			throw new RuntimeException("Service instance has not been deleted.");
		}
	}
}
