package io.confluent.servicebroker.kstreams.transformer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.Transformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisionResult;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.CreateServiceInstanceListener;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.model.CreateKafkaServiceInstanceRequest;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.model.ServiceInstance.State;

public class CreateServiceInstanceTransformer
		implements Transformer<String, CreateKafkaServiceInstanceRequest, KeyValue<String, ServiceInstance>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateServiceInstanceTransformer.class);

	private final CreateServiceInstanceListener listener;
	private final ServiceInstanceStoreConfiguration storeConfiguration;
	private final Map<ProvisionerType, ClusterProvisioner> providers;

	private KeyValueStore<String, ServiceInstance> store;

	public CreateServiceInstanceTransformer(CreateServiceInstanceListener listener,
			ServiceInstanceStoreConfiguration storeConfiguration, List<ClusterProvisioner> providers) {
		this.listener = listener;
		this.storeConfiguration = storeConfiguration;
		this.providers = providers.stream()
				.collect(Collectors.toMap(provisioner -> provisioner.getProvisionerType(), provisioner -> provisioner));
	}

	private KeyValue<String, ServiceInstance> failedResponse(String serviceInstanceId,
			ServiceInstance serviceInstance) {
		listener.notifySubscriber(serviceInstanceId, CreateServiceInstanceResponse.builder().async(true)
				.operation(OperationState.FAILED.toString()).build());

		return new KeyValue<String, ServiceInstance>(serviceInstanceId,
				new ServiceInstance(serviceInstanceId, serviceInstance.getServiceDefinitionId(),
						serviceInstance.getPlanId(), serviceInstance.getEndpoints(),
						serviceInstance.getClusterConfiguration(), State.ERROR));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		store = (KeyValueStore<String, ServiceInstance>) context.getStateStore(storeConfiguration.getName());
	}

	@Override
	public KeyValue<String, ServiceInstance> transform(String serviceInstanceId,
			CreateKafkaServiceInstanceRequest request) {
		ServiceInstance serviceInstance = new ServiceInstance(serviceInstanceId, request.getServiceDefinition().getId(),
				request.getPlan().getId(), Arrays.asList(), request.getClusterConfiguration(), State.CREATING);

		String providedClusterType = Optional.ofNullable(request.getPlan().getMetadata().get("clustertype"))
				.map(Object::toString).orElse(null);
		if (providedClusterType == null) {
			LOGGER.error("Failed to create service instance {}. Metadata \"clusterType\" is undefined",
					serviceInstanceId);

			return failedResponse(serviceInstanceId, serviceInstance);
		}

		ProvisionerType clusterProviderType;
		try {
			clusterProviderType = ProvisionerType.valueOf(providedClusterType);
		} catch (IllegalArgumentException ex) {
			LOGGER.error(providedClusterType + " is not a valid cluster provider type", ex);

			return failedResponse(serviceInstanceId, serviceInstance);
		}

		ClusterProvisioner clusterProvider = providers.get(clusterProviderType);
		if (clusterProvider == null) {
			LOGGER.error("Failed to create service instance {}. Cluster provider {} is not supported",
					serviceInstanceId, clusterProviderType);

			return failedResponse(serviceInstanceId, serviceInstance);
		}

		store.put(serviceInstanceId, serviceInstance);

		listener.notifySubscriber(serviceInstanceId, CreateServiceInstanceResponse.builder().async(true)
				.operation(OperationState.IN_PROGRESS.toString()).build());

		try {
			ClusterProvisionResult provisionResult = clusterProvider.provision(request.getPlan().getName(),
					request.getClusterConfiguration());

			serviceInstance = new ServiceInstance(serviceInstanceId, request.getServiceDefinition().getId(),
					request.getPlan().getId(), provisionResult.getEndpoints(),
					provisionResult.getClusterConfiguration(), State.CREATED);

			store.put(serviceInstanceId, serviceInstance);

			return new KeyValue<>(serviceInstanceId, serviceInstance);
		} catch (Throwable ex) {
			LOGGER.error("Error while provisioning service instance " + serviceInstanceId, ex);

			return new KeyValue<>(serviceInstanceId, setServiceInstanceOnError(serviceInstance));
		}

	}

	@Override
	public void close() {
		LOGGER.info("Closing Service Binding Processor.");
	}

	private ServiceInstance setServiceInstanceOnError(ServiceInstance serviceInstance) {
		serviceInstance = new ServiceInstance(serviceInstance.getInstanceId(), serviceInstance.getServiceDefinitionId(),
				serviceInstance.getPlanId(), serviceInstance.getEndpoints(), serviceInstance.getClusterConfiguration(),
				State.ERROR);

		store.put(serviceInstance.getInstanceId(), serviceInstance);

		return serviceInstance;
	}
}
