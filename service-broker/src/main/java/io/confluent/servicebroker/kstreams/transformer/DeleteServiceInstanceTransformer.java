package io.confluent.servicebroker.kstreams.transformer;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.kafka.streams.kstream.ValueTransformer;
import org.apache.kafka.streams.processor.ProcessorContext;
import org.apache.kafka.streams.state.KeyValueStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import io.confluent.servicebroker.controlpane.provisioning.cluster.ClusterProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.cluster.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.DeleteServiceInstanceListener;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.model.DeleteKafkaServiceInstanceRequest;
import io.confluent.servicebroker.model.DeleteServiceInstanceResult;
import io.confluent.servicebroker.model.ServiceInstance;

public class DeleteServiceInstanceTransformer
		implements ValueTransformer<DeleteKafkaServiceInstanceRequest, DeleteServiceInstanceResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeleteServiceInstanceTransformer.class);

	private final DeleteServiceInstanceListener listener;
	private final ServiceInstanceStoreConfiguration storeConfiguration;
	private final Map<ProvisionerType, ClusterProvisioner> providers;

	private KeyValueStore<String, ServiceInstance> store;

	public DeleteServiceInstanceTransformer(DeleteServiceInstanceListener listener,
			ServiceInstanceStoreConfiguration storeConfiguration, List<ClusterProvisioner> providers) {
		this.listener = listener;
		this.storeConfiguration = storeConfiguration;
		this.providers = providers.stream()
				.collect(Collectors.toMap(provisioner -> provisioner.getProvisionerType(), provisioner -> provisioner));

	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		store = (KeyValueStore<String, ServiceInstance>) context.getStateStore(storeConfiguration.getName());
	}

	private DeleteServiceInstanceResult failedResponse(String serviceInstanceId) {
		listener.notifySubscriber(serviceInstanceId, DeleteServiceInstanceResponse.builder().async(true)
				.operation(OperationState.FAILED.toString()).build());

		return new DeleteServiceInstanceResult(false);
	}

	@Override
	public DeleteServiceInstanceResult transform(DeleteKafkaServiceInstanceRequest request) {
		String providedClusterType = Optional.ofNullable(request.getPlan().getMetadata().get("clustertype"))
				.map(Object::toString).orElse(null);
		if (providedClusterType == null) {
			LOGGER.error("Failed to delete service instance {}. Metadata \"clusterType\" is undefined",
					request.getServiceInstanceId());

			return failedResponse(request.getServiceInstanceId());
		}
		ProvisionerType clusterProviderType;
		try {
			clusterProviderType = ProvisionerType.valueOf(providedClusterType);
		} catch (IllegalArgumentException ex) {
			LOGGER.error(providedClusterType + " is not a valid cluster provider type", ex);

			return failedResponse(request.getServiceInstanceId());
		}

		ClusterProvisioner clusterProvider = providers.get(clusterProviderType);
		if (clusterProvider == null) {
			LOGGER.error("Failed to delete service instance {}. Cluster provider {} is not supported",
					request.getServiceInstanceId(), clusterProviderType);

			return failedResponse(request.getServiceInstanceId());
		}

		ServiceInstance serviceInstance = store.get(request.getServiceInstanceId());
		if (serviceInstance == null) {
			// Open Service Broker API must return success if the binding does not exists.
			listener.notifySubscriber(request.getServiceInstanceId(),
					DeleteServiceInstanceResponse.builder().operation(OperationState.SUCCEEDED.toString()).build());

			return new DeleteServiceInstanceResult(false);
		}

		listener.notifySubscriber(request.getServiceInstanceId(), DeleteServiceInstanceResponse.builder().async(true)
				.operation(OperationState.IN_PROGRESS.toString()).build());

		try {
			clusterProvider.deprovision(request.getPlan().getName(), serviceInstance.getClusterConfiguration());

			store.delete(request.getServiceInstanceId());

			return new DeleteServiceInstanceResult(true);
		} catch (Throwable ex) {
			LOGGER.error("Failed to delete service instance " + request.getServiceInstanceId(), ex);

			return failedResponse(request.getServiceInstanceId());
		}
	}

	@Override
	public void close() {
		LOGGER.info("Closing Service Binding Processor.");
	}
}
