package io.confluent.servicebroker.repository;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingRequest;
import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;

import io.confluent.servicebroker.model.ServiceBinding;
import reactor.core.publisher.Mono;

public interface ServiceInstanceBindingRepository {
	Mono<CreateServiceInstanceAppBindingResponse> create(CreateServiceInstanceBindingRequest request);

	Mono<DeleteServiceInstanceBindingResponse> delete(DeleteServiceInstanceBindingRequest request);

	Mono<ServiceBinding> findOne(String serviceBindingId);
}
