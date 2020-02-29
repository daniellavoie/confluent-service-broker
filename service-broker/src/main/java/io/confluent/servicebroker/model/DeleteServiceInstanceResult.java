package io.confluent.servicebroker.model;

public class DeleteServiceInstanceResult {
	private final boolean success;

	public DeleteServiceInstanceResult(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}
}
