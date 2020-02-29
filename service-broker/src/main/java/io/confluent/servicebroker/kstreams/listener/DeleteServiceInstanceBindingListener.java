package io.confluent.servicebroker.kstreams.listener;

import org.springframework.cloud.servicebroker.model.binding.DeleteServiceInstanceBindingResponse;
import org.springframework.stereotype.Component;

@Component
public class DeleteServiceInstanceBindingListener
		extends KStreamsServiceBrokerListener<DeleteServiceInstanceBindingResponse> {

}
