package io.confluent.servicebroker.model;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceRequest;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KafkaServiceBrokerEvent {
	public enum Type {
		CREATE_SERVICE, UPDATE_SERVICE, DELETE_SERVICE, CREATE_BINDING, DELETE_BINDING
	}

	private final Type type;
	private final CreateKafkaServiceInstanceRequest createServiceInstanceRequest;
	private final UpdateServiceInstanceRequest updateServiceInstanceRequest;
	private final DeleteKafkaServiceInstanceRequest deleteServiceInstanceRequest;
	private final CreateKafkaServiceInstanceBindingRequest createServiceInstanceBindingRequest;
	private final DeleteKafkaServiceInstanceBindingRequest deleteServiceInstanceBindingRequest;

	@JsonCreator
	public KafkaServiceBrokerEvent(@JsonProperty("type") Type type,
			@JsonProperty("createServiceInstanceRequest") CreateKafkaServiceInstanceRequest createServiceInstanceRequest,
			@JsonProperty("updateServiceInstanceRequest") UpdateServiceInstanceRequest updateServiceInstanceRequest,
			@JsonProperty("deleteServiceInstanceRequest") DeleteKafkaServiceInstanceRequest deleteServiceInstanceRequest,
			@JsonProperty("createServiceInstanceBindingRequest") CreateKafkaServiceInstanceBindingRequest createServiceInstanceBindingRequest,
			@JsonProperty("deleteServiceInstanceBindingRequest") DeleteKafkaServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) {
		this.type = type;
		this.createServiceInstanceRequest = createServiceInstanceRequest;
		this.updateServiceInstanceRequest = updateServiceInstanceRequest;
		this.deleteServiceInstanceRequest = deleteServiceInstanceRequest;
		this.createServiceInstanceBindingRequest = createServiceInstanceBindingRequest;
		this.deleteServiceInstanceBindingRequest = deleteServiceInstanceBindingRequest;
	}

	public KafkaServiceBrokerEvent(CreateKafkaServiceInstanceRequest createServiceInstanceRequest) {
		this.type = Type.CREATE_SERVICE;
		this.createServiceInstanceRequest = createServiceInstanceRequest;
		this.updateServiceInstanceRequest = null;
		this.deleteServiceInstanceRequest = null;
		this.createServiceInstanceBindingRequest = null;
		this.deleteServiceInstanceBindingRequest = null;
	}

	public KafkaServiceBrokerEvent(UpdateServiceInstanceRequest udateServiceInstanceRequest) {
		this.type = Type.UPDATE_SERVICE;
		this.createServiceInstanceRequest = null;
		this.updateServiceInstanceRequest = udateServiceInstanceRequest;
		this.deleteServiceInstanceRequest = null;
		this.createServiceInstanceBindingRequest = null;
		this.deleteServiceInstanceBindingRequest = null;
	}

	public KafkaServiceBrokerEvent(DeleteKafkaServiceInstanceRequest deleteServiceInstanceRequest) {
		this.type = Type.DELETE_SERVICE;
		this.createServiceInstanceRequest = null;
		this.updateServiceInstanceRequest = null;
		this.deleteServiceInstanceRequest = deleteServiceInstanceRequest;
		this.createServiceInstanceBindingRequest = null;
		this.deleteServiceInstanceBindingRequest = null;
	}

	public KafkaServiceBrokerEvent(CreateKafkaServiceInstanceBindingRequest createServiceInstanceBindingRequest) {
		this.type = Type.CREATE_BINDING;
		this.createServiceInstanceRequest = null;
		this.updateServiceInstanceRequest = null;
		this.deleteServiceInstanceRequest = null;
		this.createServiceInstanceBindingRequest = createServiceInstanceBindingRequest;
		this.deleteServiceInstanceBindingRequest = null;
	}

	public KafkaServiceBrokerEvent(DeleteKafkaServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) {
		this.type = Type.DELETE_BINDING;
		this.createServiceInstanceRequest = null;
		this.updateServiceInstanceRequest = null;
		this.deleteServiceInstanceRequest = null;
		this.createServiceInstanceBindingRequest = null;
		this.deleteServiceInstanceBindingRequest = deleteServiceInstanceBindingRequest;
	}

	public Type getType() {
		return type;
	}

	public CreateKafkaServiceInstanceRequest getCreateServiceInstanceRequest() {
		return createServiceInstanceRequest;
	}

	public UpdateServiceInstanceRequest getUpdateServiceInstanceRequest() {
		return updateServiceInstanceRequest;
	}

	public DeleteKafkaServiceInstanceRequest getDeleteServiceInstanceRequest() {
		return deleteServiceInstanceRequest;
	}

	public CreateKafkaServiceInstanceBindingRequest getCreateServiceInstanceBindingRequest() {
		return createServiceInstanceBindingRequest;
	}

	public DeleteKafkaServiceInstanceBindingRequest getDeleteServiceInstanceBindingRequest() {
		return deleteServiceInstanceBindingRequest;
	}
}
