package io.confluent.servicebroker.kstreams.listener;

import org.springframework.cloud.servicebroker.model.instance.CreateServiceInstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class CreateServiceInstanceListener extends KStreamsServiceBrokerListener<CreateServiceInstanceResponse> {

}
