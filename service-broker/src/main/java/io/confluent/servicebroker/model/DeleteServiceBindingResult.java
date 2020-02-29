package io.confluent.servicebroker.model;

public class DeleteServiceBindingResult {
	private final boolean success;

	public DeleteServiceBindingResult(boolean success) {
		this.success = success;
	}

	public boolean isSuccess() {
		return success;
	}
}
