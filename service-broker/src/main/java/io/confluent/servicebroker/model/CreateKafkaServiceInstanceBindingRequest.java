package io.confluent.servicebroker.model;

import java.util.Map;

import org.springframework.cloud.servicebroker.model.Context;
import org.springframework.cloud.servicebroker.model.binding.BindResource;
import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.confluent.servicebroker.controlpane.model.ClientConfiguration;

public class CreateKafkaServiceInstanceBindingRequest {
	private final String serviceInstanceId;
	private final String bindingId;
	private final Context context;
	private final ServiceDefinition serviceDefinition;
	private final Plan plan;
	private final BindResource bindResource;
	private final Map<String, Object> parameters;
	private final ClientConfiguration clientConfiguration;

	@JsonCreator
	public CreateKafkaServiceInstanceBindingRequest(@JsonProperty("serviceInstanceId") String serviceInstanceId,
			@JsonProperty("bindingId") String bindingId, @JsonProperty("context") Context context,
			@JsonProperty("serviceDefinition") ServiceDefinition serviceDefinition, @JsonProperty("plan") Plan plan,
			@JsonProperty("bindResource") BindResource bindResource,
			@JsonProperty("parameters") Map<String, Object> parameters,
			@JsonProperty("clientConfiguration") ClientConfiguration clientConfiguration) {
		this.serviceInstanceId = serviceInstanceId;
		this.bindingId = bindingId;
		this.context = context;
		this.serviceDefinition = serviceDefinition;
		this.plan = plan;
		this.bindResource = bindResource;
		this.parameters = parameters;
		this.clientConfiguration = clientConfiguration;
	}

	public String getServiceInstanceId() {
		return serviceInstanceId;
	}

	public String getBindingId() {
		return bindingId;
	}

	public Context getContext() {
		return context;
	}

	public ServiceDefinition getServiceDefinition() {
		return serviceDefinition;
	}

	public Plan getPlan() {
		return plan;
	}

	public BindResource getBindResource() {
		return bindResource;
	}

	public Map<String, Object> getParameters() {
		return parameters;
	}

	public ClientConfiguration getClientConfiguration() {
		return clientConfiguration;
	}

	@Override
	public String toString() {
		return "CreateKafkaServiceInstanceBindingRequest [serviceInstanceId=" + serviceInstanceId + ", bindingId="
				+ bindingId + ", context=" + context + ", serviceDefinition=" + serviceDefinition + ", plan=" + plan
				+ ", bindResource=" + bindResource + ", parameters=" + parameters + ", clientConfiguration="
				+ clientConfiguration + "]";
	}
}
