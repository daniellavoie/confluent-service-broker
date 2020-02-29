package io.confluent.servicebroker.kstreams.listener;

import org.springframework.cloud.servicebroker.model.binding.CreateServiceInstanceAppBindingResponse;
import org.springframework.stereotype.Component;

@Component
public class CreateServiceInstanceBindingListener
		extends KStreamsServiceBrokerListener<CreateServiceInstanceAppBindingResponse> {

}
