package io.confluent.servicebroker.repository;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;
import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;

import io.confluent.servicebroker.model.ServiceInstance;
import reactor.core.publisher.Mono;

public interface ServiceInstanceRepository {
	Mono<CreateServiceInstanceResponse> create(CreateServiceInstanceRequest request);

	Mono<DeleteServiceInstanceResponse> delete(DeleteServiceInstanceRequest request);

	Mono<ServiceInstance> findOne(String serviceInstanceId);

	Mono<UpdateServiceInstanceResponse> update(UpdateServiceInstanceRequest request);
}