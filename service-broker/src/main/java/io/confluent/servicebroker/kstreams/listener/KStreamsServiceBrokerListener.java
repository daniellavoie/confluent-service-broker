package io.confluent.servicebroker.kstreams.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import reactor.core.publisher.MonoSink;

public abstract class KStreamsServiceBrokerListener<T> {
	protected final Map<String, MonoSink<T>> sinks = new HashMap<>();

	public void registerListener(String sinkKey, MonoSink<T> subscriber) {
		sinks.put(sinkKey, subscriber);
	}

	public void notifySubscriber(String key, T value) {
		Optional.ofNullable(sinks.get(key)).ifPresent(subscriber -> notifySubscriber(subscriber, value));

		sinks.remove(key);
	}

	private void notifySubscriber(MonoSink<T> subscriber, T value) {
		subscriber.success(value);
	}
}
