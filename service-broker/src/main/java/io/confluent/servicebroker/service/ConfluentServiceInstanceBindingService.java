package io.confluent.servicebroker.service;

import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.Endpoint;
import org.springframework.cloud.servicebroker.model.binding.Endpoint.Protocol;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationRequest;
import org.springframework.cloud.servicebroker.model.binding.GetLastServiceBindingOperationResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.GetServiceInstanceBindingResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.service.ServiceInstanceBindingService;
import org.springframework.stereotype.Service;

import io.confluent.servicebroker.model.ServiceBinding;
import io.confluent.servicebroker.model.ServiceBinding.State;
import io.confluent.servicebroker.repository.ServiceInstanceBindingRepository;
import reactor.core.publisher.Mono;

@Service
public class ConfluentServiceInstanceBindingService implements ServiceInstanceBindingService {
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfluentServiceInstanceBindingService.class);

	private ServiceInstanceBindingRepository repository;

	public ConfluentServiceInstanceBindingService(ServiceInstanceBindingRepository repository) {
		this.repository = repository;
	}

	private List<Endpoint> buildEndpoint(ServiceBinding serviceBinding) {
		return serviceBinding.getEndpoints().stream()

				.map(kafkaEndpoint -> Endpoint.builder()

						.host(kafkaEndpoint.getHost())

						.protocol(Protocol.TCP)

						.ports(kafkaEndpoint.getPort())

						.build())

				.collect(Collectors.toList());
	}

	@Override
	public Mono<CreateServiceInstanceBindingResponse> createServiceInstanceBinding(
			CreateServiceInstanceBindingRequest request) {
		LOGGER.debug("Processing create service instance binding {}", request);

		return repository.create(request)

				.map(response -> response);
	}

	@Override
	public Mono<GetLastServiceBindingOperationResponse> getLastOperation(
			GetLastServiceBindingOperationRequest request) {
		return repository.findOne(request.getBindingId())

				.map(serviceBinding -> GetLastServiceBindingOperationResponse.builder()

						.deleteOperation(State.DELETING.equals(serviceBinding.getState()))

						.operationState(getState(serviceBinding)).build());
	}

	@Override
	public Mono<GetServiceInstanceBindingResponse> getServiceInstanceBinding(GetServiceInstanceBindingRequest request) {
		return repository.findOne(request.getBindingId())

				.map(serviceBinding -> GetServiceInstanceAppBindingResponse.builder()

						.credentials(serviceBinding.getCredentials())

						.endpoints(buildEndpoint(serviceBinding))

						.build());
	}

	private OperationState getState(ServiceBinding serviceBinding) {
		if (State.CREATING.equals(serviceBinding.getState()) || State.DELETING.equals(serviceBinding.getState())) {
			return OperationState.IN_PROGRESS;
		} else if (State.ERROR.equals(serviceBinding.getState())) {
			return OperationState.FAILED;
		} else {
			return OperationState.SUCCEEDED;
		}
	}

	@Override
	public Mono<DeleteServiceInstanceBindingResponse> deleteServiceInstanceBinding(
			DeleteServiceInstanceBindingRequest request) {
		return repository.delete(request);
	}
}
