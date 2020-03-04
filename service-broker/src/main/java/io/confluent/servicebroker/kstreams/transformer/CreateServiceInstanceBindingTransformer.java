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
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;

import io.confluent.servicebroker.controlpane.provisioning.account.AccountProvisioner;
import io.confluent.servicebroker.controlpane.provisioning.account.ProvisionerType;
import io.confluent.servicebroker.kstreams.listener.CreateServiceInstanceBindingListener;
import io.confluent.servicebroker.kstreams.store.ServiceBindingStoreConfiguration;
import io.confluent.servicebroker.kstreams.store.ServiceInstanceStoreConfiguration;
import io.confluent.servicebroker.model.CreateKafkaServiceInstanceBindingRequest;
import io.confluent.servicebroker.model.CreateServiceBindingResult;
import io.confluent.servicebroker.model.ServiceBinding;
import io.confluent.servicebroker.model.ServiceBinding.State;
import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.principal.PrincipalProvider;

public class CreateServiceInstanceBindingTransformer implements
		Transformer<String, CreateKafkaServiceInstanceBindingRequest, KeyValue<String, CreateServiceBindingResult>> {
	private static final Logger LOGGER = LoggerFactory.getLogger(CreateServiceInstanceBindingTransformer.class);

	private final CreateServiceInstanceBindingListener listener;
	private final ServiceBindingStoreConfiguration bindingStoreConfiguration;
	private final ServiceInstanceStoreConfiguration instanceStoreConfiguration;
	private final Map<ProvisionerType, AccountProvisioner> accountProviders;
	private final Map<String, PrincipalProvider> principalProviders;

	private KeyValueStore<String, ServiceBinding> bindingStore;
	private KeyValueStore<String, ServiceInstance> instanceStore;

	public CreateServiceInstanceBindingTransformer(CreateServiceInstanceBindingListener listener,
			ServiceBindingStoreConfiguration bindingStoreConfiguration,
			ServiceInstanceStoreConfiguration instanceStoreConfiguration, List<AccountProvisioner> accountProviders,
			List<PrincipalProvider> principalProviders) {
		this.listener = listener;
		this.bindingStoreConfiguration = bindingStoreConfiguration;
		this.instanceStoreConfiguration = instanceStoreConfiguration;
		this.accountProviders = accountProviders.stream().collect(
				Collectors.toMap(AccountProvisioner::getProvisionerType, accountProvisioner -> accountProvisioner));
		this.principalProviders = principalProviders.stream().collect(
				Collectors.toMap(PrincipalProvider::getSupportedPlatform, principalProvider -> principalProvider));
	}

	private KeyValue<String, CreateServiceBindingResult> failedResponse(String serviceInstanceId, String bindingId) {
		CreateServiceInstanceAppBindingResponse response = CreateServiceInstanceAppBindingResponse.builder()

				.operation(OperationState.FAILED.toString())

				.build();

		listener.notifySubscriber(bindingId, response);

		return new KeyValue<>(serviceInstanceId, new CreateServiceBindingResult(false, null));
	}

	@Override
	@SuppressWarnings("unchecked")
	public void init(ProcessorContext context) {
		bindingStore = (KeyValueStore<String, ServiceBinding>) context
				.getStateStore(bindingStoreConfiguration.getName());
		instanceStore = (KeyValueStore<String, ServiceInstance>) context
				.getStateStore(instanceStoreConfiguration.getName());
	}

	@Override
	public KeyValue<String, CreateServiceBindingResult> transform(String serviceInstanceId,
			CreateKafkaServiceInstanceBindingRequest request) {
		String providedProvidionerType = Optional.ofNullable(request.getPlan().getMetadata().get("accountprovider"))
				.map(Object::toString).orElse(null);
		if (providedProvidionerType == null) {
			LOGGER.error("Plan {} for service {} is invalid. Metadata \"accountProvider\" is undefined",
					request.getPlan().getName(), request.getServiceDefinition().getName());

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		ProvisionerType accountProviderType;
		try {
			accountProviderType = ProvisionerType.valueOf(providedProvidionerType);
		} catch (IllegalArgumentException ex) {
			LOGGER.error(providedProvidionerType + " is not a valid account provider type", ex);

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		AccountProvisioner accountProvider = accountProviders.get(accountProviderType);
		if (accountProvider == null) {
			LOGGER.error("Failed to to proccess binding {}. Account provider {} is not supported",
					request.getBindingId(), accountProviderType);

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		String requestPrincipalProvider = request.getContext().getPlatform();
		if (requestPrincipalProvider == null) {
			LOGGER.error("Platform is undefined in the request context.");

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		PrincipalProvider principalProvider = principalProviders.get(requestPrincipalProvider);
		if (principalProvider == null) {
			LOGGER.error("No principal provider are available for {} platform.", requestPrincipalProvider);

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		String principal;
		try {
			principal = principalProvider.extractPrincipal(request).block();
		} catch (Throwable ex) {
			LOGGER.error("Could not extract principal from {}.", request);

			LOGGER.error("Failed to create binding " + request.getBindingId()
					+ ". Principal could not be extracted from request.", ex);

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		ServiceInstance serviceInstance = Optional.ofNullable(instanceStore.get(request.getServiceInstanceId()))
				.orElse(null);

		if (serviceInstance == null) {
			LOGGER.error("Failed to to proccess binding {}. Service instance {} does not exist", request.getBindingId(),
					request.getServiceInstanceId());

			return failedResponse(request.getServiceInstanceId(), request.getBindingId());
		}

		listener.notifySubscriber(request.getBindingId(), CreateServiceInstanceAppBindingResponse.builder()

				.operation(OperationState.IN_PROGRESS.toString())

				.async(true)

				.build());

		try {
			Map<String, Object> credentials = accountProvider.provisionCredentials(principal,
					request.getPlan().getName(), serviceInstance.getClusterConfiguration(),
					request.getClientConfiguration());

			ServiceBinding serviceBinding = new ServiceBinding(request.getBindingId(), principal,
					serviceInstance.getEndpoints(), credentials, request.getParameters(), State.CREATED);

			bindingStore.put(request.getBindingId(), serviceBinding);

			return new KeyValue<>(request.getBindingId(), new CreateServiceBindingResult(true, serviceBinding));
		} catch (Throwable ex) {
			LOGGER.error("Failed to create binding " + request.getBindingId() + " for service instance "
					+ request.getServiceInstanceId(), ex);

			bindingStore.put(request.getBindingId(), new ServiceBinding(request.getBindingId(), principal,
					serviceInstance.getEndpoints(), null, request.getParameters(), State.ERROR));

			return new KeyValue<>(request.getServiceInstanceId(), new CreateServiceBindingResult(false, null));
		}
	}

	@Override
	public void close() {
		LOGGER.info("Closing Service Binding Processor.");
	}
}
