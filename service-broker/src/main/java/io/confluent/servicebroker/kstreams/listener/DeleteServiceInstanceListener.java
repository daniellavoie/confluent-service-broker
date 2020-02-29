package io.confluent.servicebroker.kstreams.listener;

import org.springframework.cloud.servicebroker.model.instance.DeleteServiceInstanceResponse;
import org.springframework.stereotype.Component;

@Component
public class DeleteServiceInstanceListener extends KStreamsServiceBrokerListener<DeleteServiceInstanceResponse> {

}
