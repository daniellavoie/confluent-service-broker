package io.confluent.servicebroker.kstreams.listener;

import org.springframework.stereotype.Component;

@Component
public class ServiceBrokerListeners {
	private final CreateServiceInstanceBindingListener createServiceInstanceBindingListener;
	private final CreateServiceInstanceListener createServiceInstanceListener;
	private final DeleteServiceInstanceBindingListener deleteServiceInstanceBindingListener;
	private final DeleteServiceInstanceListener deleteServiceInstanceListener;
	private final UpdateServiceInstanceListener updateServiceInstanceListener;

	public ServiceBrokerListeners(CreateServiceInstanceBindingListener createServiceInstanceBindingListener,
			CreateServiceInstanceListener createServiceInstanceListener,
			DeleteServiceInstanceBindingListener deleteServiceInstanceBindingListener,
			DeleteServiceInstanceListener deleteServiceInstanceListener,
			UpdateServiceInstanceListener updateServiceInstanceListener) {
		this.createServiceInstanceBindingListener = createServiceInstanceBindingListener;
		this.createServiceInstanceListener = createServiceInstanceListener;
		this.deleteServiceInstanceBindingListener = deleteServiceInstanceBindingListener;
		this.deleteServiceInstanceListener = deleteServiceInstanceListener;
		this.updateServiceInstanceListener = updateServiceInstanceListener;
	}

	public CreateServiceInstanceBindingListener getCreateServiceInstanceBindingListener() {
		return createServiceInstanceBindingListener;
	}

	public CreateServiceInstanceListener getCreateServiceInstanceListener() {
		return createServiceInstanceListener;
	}

	public DeleteServiceInstanceBindingListener getDeleteServiceInstanceBindingListener() {
		return deleteServiceInstanceBindingListener;
	}

	public DeleteServiceInstanceListener getDeleteServiceInstanceListener() {
		return deleteServiceInstanceListener;
	}

	public UpdateServiceInstanceListener getUpdateServiceInstanceListener() {
		return updateServiceInstanceListener;
	}
}
