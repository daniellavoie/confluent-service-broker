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
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import io.confluent.servicebroker.controlpane.provisioning.account.AccountProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.account.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.DeleteServiceInstanceBindingListener;
import io.confluent.servicebroker.kstreams.store.ServiceBindingStoreConfiguration;
import io.confluent.servicebroker.model.DeleteKafkaServiceInstanceBindingRequest;
import io.confluent.servicebroker.model.DeleteServiceBindingResult;
import io.confluent.servicebroker.model.ServiceBinding;

public class DeleteServiceInstanceBindingTransformer
		implements ValueTransformer<DeleteKafkaServiceInstanceBindingRequest, DeleteServiceBindingResult> {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeleteServiceInstanceBindingTransformer.class);

	private final DeleteServiceInstanceBindingListener listener;
	private final ServiceBindingStoreConfiguration storeConfiguration;
	private final Map<ProvisionerType, AccountProvisioner> providers;

	private KeyValueStore<String, ServiceBinding> store;

	public DeleteServiceInstanceBindingTransformer(DeleteServiceInstanceBindingListener listener,
			ServiceBindingStoreConfiguration storeConfiguration, List<AccountProvisioner> providers) {
		this.listener = listener;
		this.storeConfiguration = storeConfiguration;
		this.providers = providers.stream().collect(
				Collectors.toMap(AccountProvisioner::getProvisionerType, accountProvisioner -> accountProvisioner));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		store = (KeyValueStore<String, ServiceBinding>) context.getStateStore(storeConfiguration.getName());
	}

	private DeleteServiceBindingResult failedResponse(String bindingId) {
		listener.notifySubscriber(bindingId, DeleteServiceInstanceBindingResponse.builder().async(true)
				.operation(OperationState.FAILED.toString()).build());

		return new DeleteServiceBindingResult(false);
	}

	@Override
	public DeleteServiceBindingResult transform(DeleteKafkaServiceInstanceBindingRequest value) {
		String providedProvidionerType = Optional.ofNullable(value.getPlan().getMetadata().get("accountprovider"))
				.map(Object::toString).orElse(null);
		if (providedProvidionerType == null) {
			LOGGER.error("Plan {} for service {} is invalid. Metadata \"accountProvider\" is undefined",
					value.getPlan().getName(), value.getServiceDefinition().getName());

			return failedResponse(value.getBindingId());
		}

		ProvisionerType accountProviderType;
		try {
			accountProviderType = ProvisionerType.valueOf(providedProvidionerType);
		} catch (IllegalArgumentException ex) {
			LOGGER.error(providedProvidionerType + " is not a valid account provider type", ex);

			return failedResponse(value.getBindingId());
		}

		AccountProvisioner accountProvider = providers.get(accountProviderType);
		if (accountProvider == null) {
			LOGGER.error("Failed to to delete binding {}. Account provider {} is not supported", value.getBindingId(),
					accountProviderType);

			return failedResponse(value.getBindingId());
		}

		ServiceBinding serviceBinding = store.get(value.getBindingId());
		if (serviceBinding == null) {
			// Open Service Broker API must return success if the binding does not exists.
			listener.notifySubscriber(value.getBindingId(), DeleteServiceInstanceBindingResponse.builder()
					.operation(OperationState.SUCCEEDED.toString()).build());

			return new DeleteServiceBindingResult(false);
		}

		listener.notifySubscriber(value.getBindingId(), DeleteServiceInstanceBindingResponse.builder().async(true)
				.operation(OperationState.IN_PROGRESS.toString()).build());

		try {
			accountProvider.deprovisionCredentials(serviceBinding.getPrincipal(), value.getPlan().getName());

			store.delete(value.getBindingId());

			return new DeleteServiceBindingResult(true);
		} catch (Throwable ex) {
			LOGGER.error("Failed to delete binding " + value.getBindingId() + " for service instance "
					+ value.getServiceInstanceId(), ex);

			return failedResponse(value.getBindingId());
		}
	}

	@Override
	public void close() {
		LOGGER.info("Closing Service Binding Processor.");
	}
}
