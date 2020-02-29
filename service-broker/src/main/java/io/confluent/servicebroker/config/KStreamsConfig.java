package io.confluent.servicebroker.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.state.Stores;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.confluent.servicebroker.controlpane.provisioning.account.AccountProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisioner;
import io.confluent.servicebroker.kstreams.listener.ServiceBrokerListeners;
import io.confluent.servicebroker.kstreams.store.ServiceBindingStoreConfiguration;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.kstreams.topic.KafkaServiceBrokerEventTopicConfiguration;
import io.confluent.servicebroker.kstreams.topic.TopicConfiguration;
import io.confluent.servicebroker.kstreams.transformer.CreateServiceInstanceBindingTransformer;
import io.confluent.servicebroker.kstreams.transformer.CreateServiceInstanceTransformer;
import io.confluent.servicebroker.kstreams.transformer.DeleteServiceInstanceBindingTransformer;
import io.confluent.servicebroker.kstreams.transformer.DeleteServiceInstanceTransformer;
import io.confluent.servicebroker.kstreams.transformer.UpdateServiceInstanceTransformer;
import io.confluent.servicebroker.model.KafkaServiceBrokerEvent;
import io.confluent.servicebroker.model.KafkaServiceBrokerEvent.Type;
import io.confluent.servicebroker.model.ServiceBinding;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.principal.PrincipalProvider;

@Configuration
public class KStreamsConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger(KStreamsConfig.class);
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().findAndRegisterModules();

	private final ServiceBrokerListeners listeners;
	private final KafkaServiceBrokerEventTopicConfiguration topicConfiguration;
	private final ServiceBindingStoreConfiguration serviceBindingStoreConfiguration;
	private final ServiceInstanceStoreConfiguration serviceInstanceStoreConfiguration;
	private final StreamsBuilder streamsBuilder;
	private final List<ClusterProvisioner> clusterProvisioners;
	private final List<AccountProvisioner> accountProviders;
	private final List<PrincipalProvider> principalProviders;

	public KStreamsConfig(ServiceBrokerListeners listeners,
			KafkaServiceBrokerEventTopicConfiguration topicConfiguration,
			ServiceBindingStoreConfiguration serviceBindingStoreConfiguration,
			ServiceInstanceStoreConfiguration serviceInstanceStoreConfiguration, StreamsBuilder streamsBuilder,
			List<ClusterProvisioner> clusterProvisioners, List<AccountProvisioner> accountProviders,
			List<PrincipalProvider> principalProviders) {
		this.listeners = listeners;

		this.topicConfiguration = topicConfiguration;
		this.serviceBindingStoreConfiguration = serviceBindingStoreConfiguration;
		this.serviceInstanceStoreConfiguration = serviceInstanceStoreConfiguration;
		this.streamsBuilder = streamsBuilder;

		this.clusterProvisioners = clusterProvisioners;
		this.accountProviders = accountProviders;
		this.principalProviders = principalProviders;
	}

	private void createTopicIfMissing(TopicConfiguration topicConfiguration, AdminClient adminClient) {
		try {
			if (!adminClient.listTopics().names().get().stream()
					.filter(existingTopic -> existingTopic.equals(topicConfiguration.getName())).findAny()
					.isPresent()) {
				LOGGER.info("Creating topic {}.", topicConfiguration.getName());

				NewTopic topic = new NewTopic(topicConfiguration.getName(), topicConfiguration.getPartitions(),
						topicConfiguration.getReplicationFactor());

				if (topicConfiguration.isCompacted()) {
					topic.configs(new HashMap<>());
					topic.configs().put(TopicConfig.CLEANUP_POLICY_CONFIG, TopicConfig.CLEANUP_POLICY_COMPACT);
				}

				adminClient.createTopics(Arrays.asList(topic)).all().get();
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private <T> void setupStateStore(String name, Class<T> objectType) {
		streamsBuilder.addStateStore(Stores.keyValueStoreBuilder(Stores.persistentKeyValueStore(name), Serdes.String(),
				new JsonSerde<T>(objectType, OBJECT_MAPPER)));
	}

	@Bean
	public Topology topology(AdminClient adminClient) {
		createTopicIfMissing(topicConfiguration, adminClient);

		setupStateStore(serviceBindingStoreConfiguration.getName(), ServiceBinding.class);
		setupStateStore(serviceInstanceStoreConfiguration.getName(), ServiceInstance.class);

		KStream<String, KafkaServiceBrokerEvent> eventStream = streamsBuilder
				.<String, KafkaServiceBrokerEvent>stream(topicConfiguration.getName());

		configureCreateServiceInstanceStream(eventStream);
		configureDeleteServiceInstanceStream(eventStream);
		configureUpdateServiceInstanceStream(eventStream);
		configureCreateServiceInstanceBindingStream(eventStream);
		configureDeleteServiceInstanceBindingStream(eventStream);

		return streamsBuilder.build();
	}

	private void configureCreateServiceInstanceBindingStream(KStream<String, KafkaServiceBrokerEvent> eventStream) {
		eventStream

				.filter((serviceInstanceId, event) -> Type.CREATE_BINDING.equals(event.getType()))

				.mapValues(KafkaServiceBrokerEvent::getCreateServiceInstanceBindingRequest)

				.transform(
						() -> new CreateServiceInstanceBindingTransformer(
								listeners.getCreateServiceInstanceBindingListener(), serviceBindingStoreConfiguration,
								serviceInstanceStoreConfiguration, accountProviders, principalProviders),
						serviceBindingStoreConfiguration.getName(), serviceInstanceStoreConfiguration.getName());
	}

	private void configureCreateServiceInstanceStream(KStream<String, KafkaServiceBrokerEvent> eventStream) {
		eventStream

				.filter((serviceInstanceId, event) -> Type.CREATE_SERVICE.equals(event.getType()))

				.mapValues(KafkaServiceBrokerEvent::getCreateServiceInstanceRequest)

				.transform(
						() -> new CreateServiceInstanceTransformer(listeners.getCreateServiceInstanceListener(),
								serviceInstanceStoreConfiguration, clusterProvisioners),
						serviceInstanceStoreConfiguration.getName());
	}

	private void configureDeleteServiceInstanceBindingStream(KStream<String, KafkaServiceBrokerEvent> eventStream) {
		eventStream

				.filter((serviceInstanceId, event) -> Type.DELETE_BINDING.equals(event.getType()))

				.mapValues(KafkaServiceBrokerEvent::getDeleteServiceInstanceBindingRequest)

				.transformValues(() -> new DeleteServiceInstanceBindingTransformer(
						listeners.getDeleteServiceInstanceBindingListener(), serviceBindingStoreConfiguration,
						accountProviders), serviceBindingStoreConfiguration.getName());
	}

	private void configureDeleteServiceInstanceStream(KStream<String, KafkaServiceBrokerEvent> eventStream) {
		eventStream

				.filter((serviceInstanceId, event) -> Type.DELETE_SERVICE.equals(event.getType()))

				.mapValues(KafkaServiceBrokerEvent::getDeleteServiceInstanceRequest)

				.transformValues(
						() -> new DeleteServiceInstanceTransformer(listeners.getDeleteServiceInstanceListener(),
								serviceInstanceStoreConfiguration, clusterProvisioners),
						serviceInstanceStoreConfiguration.getName());
	}

	private void configureUpdateServiceInstanceStream(KStream<String, KafkaServiceBrokerEvent> eventStream) {
		eventStream

				.filter((serviceInstanceId, event) -> Type.UPDATE_SERVICE.equals(event.getType()))

				.mapValues(KafkaServiceBrokerEvent::getUpdateServiceInstanceRequest)

				.transform(
						() -> new UpdateServiceInstanceTransformer(listeners.getUpdateServiceInstanceListener(),
								serviceInstanceStoreConfiguration, clusterProvisioners),
						serviceInstanceStoreConfiguration.getName());
	}
}
