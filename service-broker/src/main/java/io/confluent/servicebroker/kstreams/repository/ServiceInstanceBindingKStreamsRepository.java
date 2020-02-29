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
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingDoesNotExistException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceBindingExistsException;
import org.springframework.cloud.servicebroker.exception.ServiceInstanceDoesNotExistException;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.kafka.config.StreamsBuilderFactoryBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Repository;

import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClientConfiguration;
import io.confluent.servicebroker.controlpane.provisioning.account.AccountProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.account.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.ServiceBrokerListeners;
import io.confluent.servicebroker.kstreams.store.ServiceBindingStoreConfiguration;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.kstreams.topic.KafkaServiceBrokerEventTopicConfiguration;
import io.confluent.servicebroker.model.CreateKafkaServiceInstanceBindingRequest;
import io.confluent.servicebroker.model.DeleteKafkaServiceInstanceBindingRequest;
import io.confluent.servicebroker.model.KafkaServiceBrokerEvent;
import io.confluent.servicebroker.model.ServiceBinding;
import io.confluent.servicebroker.model.ServiceBinding.State;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.repository.ServiceInstanceBindingRepository;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

@Repository
public class ServiceInstanceBindingKStreamsRepository implements ServiceInstanceBindingRepository {
	private final ServiceBrokerListeners listeners;
	private final Map<ProvisionerType, AccountProvisioner> provisioners;
	private final KafkaServiceBrokerEventTopicConfiguration topicConfiguration;
	private final StreamsBuilderFactoryBean streamsBuilderFactoryBean;
	private final ServiceBindingStoreConfiguration serviceBindingStoreConfiguration;
	private final ServiceInstanceStoreConfiguration serviceInstanceStoreConfiguration;
	private final KafkaTemplate<String, KafkaServiceBrokerEvent> eventTemplate;

	private ReadOnlyKeyValueStore<String, ServiceBinding> serviceBindingStore;
	private ReadOnlyKeyValueStore<String, ServiceInstance> serviceInstanceStore;

	public ServiceInstanceBindingKStreamsRepository(ServiceBrokerListeners listeners,
			List<AccountProvisioner> provisioners, KafkaServiceBrokerEventTopicConfiguration topicConfiguration,
			StreamsBuilderFactoryBean streamsBuilderFactoryBean,
			ServiceBindingStoreConfiguration serviceBindingStoreConfiguration,
			ServiceInstanceStoreConfiguration serviceInstanceStoreConfiguration,
			KafkaTemplate<String, KafkaServiceBrokerEvent> eventTemplate) {
		this.listeners = listeners;
		this.topicConfiguration = topicConfiguration;
		this.streamsBuilderFactoryBean = streamsBuilderFactoryBean;
		this.serviceBindingStoreConfiguration = serviceBindingStoreConfiguration;
		this.serviceInstanceStoreConfiguration = serviceInstanceStoreConfiguration;
		this.eventTemplate = eventTemplate;

		this.provisioners = provisioners.stream().collect(
				Collectors.toMap(AccountProvisioner::getProvisionerType, accountProvisioner -> accountProvisioner));
	}

	@Override
	public Mono<CreateServiceInstanceAppBindingResponse> create(CreateServiceInstanceBindingRequest request) {
		ServiceBinding existingServiceBinding = getServiceBindingStore().get(request.getBindingId());

		if (existingServiceBinding != null) {
			if (State.CREATED.equals(existingServiceBinding.getState())) {
				return Mono.error(new ServiceInstanceBindingExistsException(request.getServiceInstanceId(),
						request.getBindingId()));
			} else {
				return Mono.error(new ServiceBrokerCreateOperationInProgressException());
			}
		}

		if (!request.isAsyncAccepted()) {
			return Mono
					.error(new ServiceBrokerAsyncRequiredException("Request needs to accept asynchronous operation."));
		}

		ProvisionerType clusterType = ProvisionerType
				.valueOf(request.getPlan().getMetadata().get("clustertype").toString());

		AccountProvisioner accountProvisioner = Optional.ofNullable(provisioners.get(clusterType)).orElseThrow(
				() -> new IllegalArgumentException("Could not find an account provisioner for " + clusterType));

		Optional.ofNullable(getServiceInstanceStore().get(request.getServiceInstanceId()))
				.orElseThrow(() -> new ServiceInstanceDoesNotExistException(request.getServiceInstanceId()));

		try {
			accountProvisioner
					.assertAccountProviderConfiguration(request.getParameters(AccountProviderConfiguration.class));
		} catch (Throwable ex) {
			throw new ServiceBrokerInvalidParametersException(ex);
		}

		return Mono.create(subscriber -> create(request, subscriber));
	}

	private void create(CreateServiceInstanceBindingRequest request,
			MonoSink<CreateServiceInstanceAppBindingResponse> subscriber) {
		listeners.getCreateServiceInstanceBindingListener().registerListener(request.getBindingId(), subscriber);

		Mono.fromFuture(eventTemplate
				.send(topicConfiguration.getName(), request.getServiceInstanceId(),
						new KafkaServiceBrokerEvent(toKafkaRequest(request)))

				.completable())

				.doOnError(subscriber::error)

				.subscribe();
	}

	private void delete(DeleteServiceInstanceBindingRequest request,
			MonoSink<DeleteServiceInstanceBindingResponse> subscriber) {
		listeners.getDeleteServiceInstanceBindingListener().registerListener(request.getBindingId(), subscriber);

		Mono.fromFuture(eventTemplate
				.send(topicConfiguration.getName(), request.getServiceInstanceId(),
						new KafkaServiceBrokerEvent(toKafkaRequest(request)))

				.completable())

				.doOnError(subscriber::error)

				.subscribe();
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> delete(DeleteServiceInstanceBindingRequest request) {
		ServiceBinding existingServiceBinding = getServiceBindingStore().get(request.getBindingId());
		if (existingServiceBinding == null) {
			return Mono.error(new ServiceInstanceBindingDoesNotExistException(request.getBindingId()));
		}

		if (!State.CREATED.equals(existingServiceBinding.getState())) {
			return Mono.error(new ServiceBrokerDeleteOperationInProgressException());
		}

		if (!request.isAsyncAccepted()) {
			return Mono
					.error(new ServiceBrokerAsyncRequiredException("Request needs to accept asynchronous operation."));
		}

		return Mono.create(subscriber -> delete(request, subscriber));
	}

	@Override
	public Mono<ServiceBinding> findOne(String instanceBindingId) {
		return Optional.ofNullable(getServiceBindingStore().get(instanceBindingId)).map(Mono::just)
				.orElseGet(Mono::empty);
	}

	private ReadOnlyKeyValueStore<String, ServiceBinding> getServiceBindingStore() {
		if (serviceBindingStore == null) {
			serviceBindingStore = streamsBuilderFactoryBean.getKafkaStreams().store(
					serviceBindingStoreConfiguration.getName(),
					QueryableStoreTypes.<String, ServiceBinding>keyValueStore());
		}

		return serviceBindingStore;
	}

	private ReadOnlyKeyValueStore<String, ServiceInstance> getServiceInstanceStore() {
		if (serviceInstanceStore == null) {
			serviceInstanceStore = streamsBuilderFactoryBean.getKafkaStreams().store(
					serviceInstanceStoreConfiguration.getName(),
					QueryableStoreTypes.<String, ServiceInstance>keyValueStore());
		}

		return serviceInstanceStore;
	}

	private CreateKafkaServiceInstanceBindingRequest toKafkaRequest(CreateServiceInstanceBindingRequest request) {
		ClientConfiguration clientConfiguration = request.getParameters(ClientConfiguration.class);

		return new CreateKafkaServiceInstanceBindingRequest(request.getServiceInstanceId(), request.getBindingId(),
				request.getContext(), request.getServiceDefinition(), request.getPlan(), request.getBindResource(),
				request.getParameters(), clientConfiguration);
	}

	private DeleteKafkaServiceInstanceBindingRequest toKafkaRequest(DeleteServiceInstanceBindingRequest request) {
		return new DeleteKafkaServiceInstanceBindingRequest(request.getServiceInstanceId(), request.getBindingId(),
				request.getServiceDefinition(), request.getPlan(), request.isAsyncAccepted());
	}
}
