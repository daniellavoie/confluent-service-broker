package io.confluent.servicebroker.model;

public class CreateServiceBindingResult {
	private final boolean successful;
	private final ServiceBinding serviceBinding;
	
	public CreateServiceBindingResult(boolean successful, ServiceBinding serviceBinding) {
		this.successful = successful;
		this.serviceBinding = serviceBinding;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public ServiceBinding getServiceBinding() {
		return serviceBinding;
	}
}
