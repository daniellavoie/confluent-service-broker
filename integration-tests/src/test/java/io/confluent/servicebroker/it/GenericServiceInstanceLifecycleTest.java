package io.confluent.servicebroker.it;

import org.springframework.cloud.servicebroker.model.catalog.Plan;
import org.springframework.cloud.servicebroker.model.catalog.ServiceDefinition;

public class GenericServiceInstanceLifecycleTest extends ServiceInstanceLifecycleTest {
	@Override
	protected Plan getPlan(ServiceDefinition serviceDefinition) {
		return serviceDefinition.getPlans().stream()
				.filter(plan -> plan.getMetadata().get("accountprovider").equals("GENERIC")).findFirst().get();
	}
}
