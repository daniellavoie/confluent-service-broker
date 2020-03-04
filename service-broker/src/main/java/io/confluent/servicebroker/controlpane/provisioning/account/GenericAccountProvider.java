package io.confluent.servicebroker.controlpane.provisioning.account;

import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterCredentials;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

@Component
public class GenericAccountProvider extends AbstractAccountProvisioner {
	public GenericAccountProvider(ControlPaneConfiguration controlPaneConfiguration) {
		super(controlPaneConfiguration);
	}

	@Override
	public void assertAccountProviderConfiguration(AccountProviderConfiguration accountProviderConfiguration) {

	}

	@Override
	protected ClusterCredentials getCredentials(String principal,
			ClusterProviderConfiguration clusterProviderConfiguration) {
		return clusterProviderConfiguration.getGenericCredentials();
	}

	@Override
	public ProvisionerType getProvisionerType() {
		return ProvisionerType.GENERIC;
	}

	@Override
	public void removeCredentials(String principal, ClusterProviderConfiguration clusterProviderConfiguration) {
		// Nothing to do.
	}
}
