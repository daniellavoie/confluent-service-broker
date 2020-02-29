package io.confluent.servicebroker.kstreams.transformer;

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
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse.UpdateServiceInstanceResponseBuilder;

import io.confluent.servicebroker.controlpane.model.ClusterConfiguration;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisionResult;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.UpdateServiceInstanceListener;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.model.ServiceInstance.State;

public class UpdateServiceInstanceTransformer
		implements Transformer<String, UpdateServiceInstanceRequest, KeyValue<String, UpdateServiceInstanceResponse>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateServiceInstanceTransformer.class);

	private final UpdateServiceInstanceListener listener;
	private final ServiceInstanceStoreConfiguration storeConfiguration;
	private final Map<ProvisionerType, ClusterProvisioner> providers;

	private KeyValueStore<String, ServiceInstance> store;

	public UpdateServiceInstanceTransformer(UpdateServiceInstanceListener listener,
			ServiceInstanceStoreConfiguration storeConfiguration, List<ClusterProvisioner> providers) {
		this.listener = listener;
		this.storeConfiguration = storeConfiguration;
		this.providers = providers.stream()
				.collect(Collectors.toMap(provisioner -> provisioner.getProvisionerType(), provisioner -> provisioner));
	}

	private KeyValue<String, UpdateServiceInstanceResponse> failedResponse(String serviceInstanceId,
			UpdateServiceInstanceResponseBuilder responseBuilder) {
		UpdateServiceInstanceResponse response = responseBuilder.operation(OperationState.FAILED.toString()).build();

		listener.notifySubscriber(serviceInstanceId, response);

		return new KeyValue<>(serviceInstanceId, responseBuilder.build());
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		store = (KeyValueStore<String, ServiceInstance>) context.getStateStore(storeConfiguration.getName());
	}

	@Override
	public KeyValue<String, UpdateServiceInstanceResponse> transform(String serviceInstanceId,
			UpdateServiceInstanceRequest request) {
		UpdateServiceInstanceResponseBuilder responseBuilder = UpdateServiceInstanceResponse.builder().async(true)
				.operation(OperationState.IN_PROGRESS.toString());

		String providedClusterType = Optional.ofNullable(request.getPlan().getMetadata().get("clustertype"))
				.map(Object::toString).orElse(null);
		if (providedClusterType == null) {
			LOGGER.error("Failed to update service instance {}. Metadata \"clusterType\" is undefined",
					request.getServiceInstanceId());

			return failedResponse(request.getServiceInstanceId(), responseBuilder);
		}

		ProvisionerType clusterProviderType;
		try {
			clusterProviderType = ProvisionerType.valueOf(providedClusterType);
		} catch (IllegalArgumentException ex) {
			LOGGER.error(providedClusterType + " is not a valid cluster provider type", ex);

			return failedResponse(request.getServiceInstanceId(), responseBuilder);
		}

		ClusterProvisioner clusterProvider = providers.get(clusterProviderType);
		if (clusterProvider == null) {
			LOGGER.error("Failed to update service instance {}. Cluster provider {} is not supported",
					request.getServiceInstanceId(), clusterProviderType);

			return failedResponse(request.getServiceInstanceId(), responseBuilder);
		}

		ServiceInstance serviceInstance = Optional.ofNullable(store.get(request.getServiceInstanceId())).orElse(null);
		if (serviceInstance == null) {
			LOGGER.error("Failed to update service instance {}. Service instance does not exist",
					request.getServiceInstanceId());

			return failedResponse(request.getServiceInstanceId(), responseBuilder);
		}

		ClusterConfiguration userConfiguration;
		try {
			userConfiguration = request.getParameters(ClusterConfiguration.class);
		} catch (Throwable ex) {
			LOGGER.error("Failed to update service instance " + request.getServiceInstanceId()
					+ ". Invalid user configurations", ex);

			return failedResponse(request.getServiceInstanceId(), responseBuilder);
		}

		store.put(request.getServiceInstanceId(), serviceInstance);

		listener.notifySubscriber(request.getServiceInstanceId(), responseBuilder.build());

		try {
			ClusterProvisionResult provisionResult = clusterProvider.update(request.getPlan().getName(),
					userConfiguration);

			store.put(request.getServiceInstanceId(),
					new ServiceInstance(request.getServiceInstanceId(), request.getServiceDefinitionId(),
							request.getPlanId(), provisionResult.getEndpoints(),
							provisionResult.getClusterConfiguration(), State.CREATED));
		} catch (Throwable ex) {
			LOGGER.error("Error while updating service instance " + request.getServiceInstanceId(), ex);

			setServiceInstanceOnError(serviceInstance);
		}

		return new KeyValue<>(request.getServiceInstanceId(), responseBuilder.build());
	}

	private void setServiceInstanceOnError(ServiceInstance serviceInstance) {
		serviceInstance = new ServiceInstance(serviceInstance.getInstanceId(), serviceInstance.getServiceDefinitionId(),
				serviceInstance.getPlanId(), serviceInstance.getEndpoints(), serviceInstance.getClusterConfiguration(),
				State.ERROR);

		store.put(serviceInstance.getInstanceId(), serviceInstance);
	}

	@Override
	public void close() {
		LOGGER.info("Closing Service Binding Processor.");
	}
}
