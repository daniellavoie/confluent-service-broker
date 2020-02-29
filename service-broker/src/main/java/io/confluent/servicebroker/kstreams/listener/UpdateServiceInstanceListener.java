package io.confluent.servicebroker.kstreams.listener;

import org.springframework.cloud.servicebroker.model.instance.UpdateServiceInstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class UpdateServiceInstanceListener extends KStreamsServiceBrokerListener<UpdateServiceInstanceResponse> {

}
