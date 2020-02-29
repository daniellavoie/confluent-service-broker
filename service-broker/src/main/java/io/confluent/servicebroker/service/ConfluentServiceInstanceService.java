package io.confluent.servicebroker.service;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationRequest;
import org.springframework.cloud.servicebroker.model.instance.GetLastServiceOperationResponse;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.GetServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.OperationState;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.service.ServiceInstanceService;
import org.springframework.stereotype.Service;

import io.confluent.servicebroker.model.ServiceInstance;
import io.confluent.servicebroker.model.ServiceInstance.State;
import io.confluent.servicebroker.repository.ServiceInstanceRepository;
import reactor.core.publisher.Mono;

@Service
public class ConfluentServiceInstanceService implements ServiceInstanceService {
	private final ServiceInstanceRepository repository;

	public ConfluentServiceInstanceService(ServiceInstanceRepository repository) {
		this.repository = repository;
	}

	@Override
	public Mono<CreateServiceInstanceResponse> createServiceInstance(CreateServiceInstanceRequest request) {
		return repository.create(request);
	}

	@Override
	public Mono<DeleteServiceInstanceResponse> deleteServiceInstance(DeleteServiceInstanceRequest request) {
		return repository.delete(request);
	}

	@Override
	public Mono<GetLastServiceOperationResponse> getLastOperation(GetLastServiceOperationRequest request) {
		return repository.findOne(request.getServiceInstanceId())

				.map(serviceInstance -> GetLastServiceOperationResponse.builder()

						.deleteOperation(State.DELETING.equals(serviceInstance.getState()))

						.operationState(getState(serviceInstance)).build());
	}

	@Override
	public Mono<GetServiceInstanceResponse> getServiceInstance(GetServiceInstanceRequest request) {
		return repository.findOne(request.getServiceInstanceId())
				.map(serviceInstance -> GetServiceInstanceResponse.builder()
						.parameters("topics", serviceInstance.getClusterConfiguration().getTopics())
						.parameters("clientProperties", serviceInstance.getClusterConfiguration().getClientProperties())
						.planId(serviceInstance.getPlanId())
						.serviceDefinitionId(serviceInstance.getServiceDefinitionId()).build());
	}

	private OperationState getState(ServiceInstance serviceInstance) {
		if (State.CREATING.equals(serviceInstance.getState()) || State.DELETING.equals(serviceInstance.getState())
				|| State.UPDATING.equals(serviceInstance.getState())) {
			return OperationState.IN_PROGRESS;
		} else if (State.ERROR.equals(serviceInstance.getState())) {
			return OperationState.FAILED;
		} else {
			return OperationState.SUCCEEDED;
		}
	}

	@Override
	public Mono<UpdateServiceInstanceResponse> updateServiceInstance(UpdateServiceInstanceRequest request) {
		return repository.update(request);
	}

}
