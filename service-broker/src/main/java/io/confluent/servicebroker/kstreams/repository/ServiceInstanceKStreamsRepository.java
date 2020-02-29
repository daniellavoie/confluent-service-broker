package io.confluent.servicebroker.kstreams.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.kafka.streams.state.QueryableStoreTypes;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerAsyncRequiredException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerCreateOperationInProgressException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerDeleteOperationInProgressException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerInvalidParametersException;
import org.springframework.cloud.servicebroker.exception.ServiceBrokerUpdateOperationInProgressException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceExistsException;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.ServiceBrokerListeners;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.kstreams.topic.KafkaServiceBrokerEventTopicConfiguration;
import io.confluent.servicebroker.model.CreateKafkaServiceInstanceRequest;
import io.confluent.servicebroker.model.DeleteKafkaServiceInstanceRequest;
import io.confluent.servicebroker.model.KafkaServiceBrokerEvent;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.model.ServiceInstance.State;
import io.confluent.servicebroker.repository.ServiceInstanceRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Repository
public class ServiceInstanceKStreamsRepository implements ServiceInstanceRepository {
	private final ServiceBrokerListeners listeners;
	private final Map<ProvisionerType, ClusterProvisioner> provisioners;
	private final KafkaServiceBrokerEventTopicConfiguration topicConfiguration;
	private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
	private final ServiceInstanceStoreConfiguration storeConfiguration;
	private final KafkaTemplate<String, KafkaServiceBrokerEvent> eventTemplate;

	private ReadOnlyKeyValueStore<String, ServiceInstance> store;

	public ServiceInstanceKStreamsRepository(ServiceBrokerListeners listeners, List<ClusterProvisioner> provisioners,
			KafkaServiceBrokerEventTopicConfiguration topicConfiguration,
			StreamsBuilderFactoryBean streamsBuilderFactoryBean, ServiceInstanceStoreConfiguration storeConfiguration,
			KafkaTemplate<String, KafkaServiceBrokerEvent> eventTemplate) {
		this.listeners = listeners;
		this.topicConfiguration = topicConfiguration;
		this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
		this.storeConfiguration = storeConfiguration;
		this.eventTemplate = eventTemplate;

		this.provisioners = provisioners.stream()
				.collect(Collectors.toMap(ClusterProvisioner::getProvisionerType, clusterProvider -> clusterProvider));
	}

	@Override
	public Mono<CreateServiceInstanceResponse> create(CreateServiceInstanceRequest request) {
		ServiceInstance existingServiceInstance = getStore().get(request.getServiceInstanceId());

		if (existingServiceInstance != null) {
			if (State.CREATED.equals(existingServiceInstance.getState())) {
				return Mono.error(new ServiceInstanceExistsException(request.getServiceInstanceId(),
						request.getServiceDefinition().getId()));
			} else {
				return Mono.error(new ServiceBrokerCreateOperationInProgressException());
			}
		}

		if (!request.isAsyncAccepted()) {
			return Mono
					.error(new ServiceBrokerAsyncRequiredException("Request needs to accept asynchronous operation."));
		}

		String providedClusterType = Optional.ofNullable(request.getPlan().getMetadata())
				.map(metadata -> metadata.get("clustertype")).map(Object::toString).orElse(null);
		if (providedClusterType == null) {
			return Mono.error(new IllegalArgumentException("Failed to create service instance "
					+ request.getServiceInstanceId() + ". Metadata \"clusterType\" is undefined"));
		}

		ProvisionerType clusterProviderType;
		try {
			clusterProviderType = ProvisionerType.valueOf(providedClusterType);
		} catch (IllegalArgumentException ex) {
			return Mono.error(
					new IllegalArgumentException(providedClusterType + " is not a valid cluster provider type", ex));
		}

		ClusterProvisioner clusterProvisioner = Optional.ofNullable(provisioners.get(clusterProviderType)).orElseThrow(
				() -> new IllegalArgumentException("Could not find a cluster provisioner for " + clusterProviderType));

		try {
			clusterProvisioner.assertUserConfigurations(request.getParameters(ClusterConfiguration.class));
		} catch (Throwable ex) {
			throw new ServiceBrokerInvalidParametersException(ex);
		}

		return Mono.create(subscriber -> create(request, subscriber));
	}

	private void create(CreateServiceInstanceRequest request, MonoSink<CreateServiceInstanceResponse> subscriber) {
		listeners.getCreateServiceInstanceListener().registerListener(request.getServiceInstanceId(), subscriber);

		Mono.fromFuture(eventTemplate
				.send(topicConfiguration.getName(), request.getServiceInstanceId(),
						new KafkaServiceBrokerEvent(toKafkaRequest(request)))

				.completable())

				.doOnError(subscriber::error)

				.subscribe();
	}

	private void delete(DeleteServiceInstanceRequest request, MonoSink<DeleteServiceInstanceResponse> subscriber) {
		listeners.getDeleteServiceInstanceListener().registerListener(request.getServiceInstanceId(), subscriber);

		Mono.fromFuture(eventTemplate
				.send(topicConfiguration.getName(), request.getServiceInstanceId(),
						new KafkaServiceBrokerEvent(toKafkaRequest(request)))

				.completable())

				.doOnError(subscriber::error)

				.subscribe();
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> delete(DeleteServiceInstanceRequest request) {
		ServiceInstance existingServiceInstance = getStore().get(request.getServiceInstanceId());
		if (existingServiceInstance == null) {
			return Mono.error(new ServiceInstanceDoesNotExistException(request.getServiceInstanceId()));
		}

		if (!State.CREATED.equals(existingServiceInstance.getState())) {
			return Mono.error(new ServiceBrokerDeleteOperationInProgressException());
		}

		if (!request.isAsyncAccepted()) {
			return Mono
					.error(new ServiceBrokerAsyncRequiredException("Request needs to accept asynchronous operation."));
		}

		return Mono.create(subscriber -> delete(request, subscriber));
	}

	@Override
	public Mono<ServiceInstance> findOne(String serviceInstanceId) {
		return Optional.ofNullable(getStore().get(serviceInstanceId)).map(Mono::just).orElseGet(Mono::empty);
	}

	private ReadOnlyKeyValueStore<String, ServiceInstance> getStore() {
		if (store == null) {
			store = streamsBuilderFactoryBean.getKafkaStreams().store(storeConfiguration.getName(),
					QueryableStoreTypes.<String, ServiceInstance>keyValueStore());
		}

		return store;
	}

	private CreateKafkaServiceInstanceRequest toKafkaRequest(CreateServiceInstanceRequest request) {
		ProvisionerType providerType = ProvisionerType
				.valueOf(request.getPlan().getMetadata().get("clustertype").toString());

		ClusterConfiguration clusterConfiguration = request.getParameters(ClusterConfiguration.class);

		return new CreateKafkaServiceInstanceRequest(request.getServiceInstanceId(), request.isAsyncAccepted(),
				request.getServiceDefinition(), request.getPlan(), clusterConfiguration, providerType);
	}

	private DeleteKafkaServiceInstanceRequest toKafkaRequest(DeleteServiceInstanceRequest request) {
		return new DeleteKafkaServiceInstanceRequest(request.getServiceInstanceId(), request.getServiceDefinition(),
				request.getPlan(), request.isAsyncAccepted());
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> update(UpdateServiceInstanceRequest request) {
		ServiceInstance existingServiceInstance = getStore().get(request.getServiceInstanceId());
		if (existingServiceInstance == null) {
			return Mono.error(new ServiceInstanceDoesNotExistException(request.getServiceInstanceId()));
		}

		if (!request.isAsyncAccepted()) {
			return Mono
					.error(new ServiceBrokerAsyncRequiredException("Request needs to accept asynchronous operation."));
		}

		if (!State.CREATED.equals(existingServiceInstance.getState())) {
			return Mono.error(new ServiceBrokerUpdateOperationInProgressException());
		}

		// TODO - Control the update parameters and throw a
		// ServiceInstanceUpdateNotSupportedException if one of them is imcompatible
		// with the current state of the service.

		return Mono.create(subscriber -> update(request, subscriber));
	}

	private void update(UpdateServiceInstanceRequest request, MonoSink<UpdateServiceInstanceResponse> subscriber) {
		listeners.getUpdateServiceInstanceListener().registerListener(request.getServiceInstanceId(), subscriber);

		Mono.fromFuture(eventTemplate.send(topicConfiguration.getName(), request.getServiceInstanceId(),
				new KafkaServiceBrokerEvent(request)).completable())

				.doOnError(subscriber::error)

				.subscribe();
		;

	}

}
