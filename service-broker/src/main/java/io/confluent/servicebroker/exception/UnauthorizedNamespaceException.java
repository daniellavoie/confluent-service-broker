package io.confluent.servicebroker.exception;

public class UnauthorizedNamespaceException extends RuntimeException {
	private static final long serialVersionUID = -1605148094626878989L;

	public UnauthorizedNamespaceException(String namespace) {
		super("Namedspace " + namespace + " is not granted any Kafka user account. Contact your Kafka administrator.");
	}
}
