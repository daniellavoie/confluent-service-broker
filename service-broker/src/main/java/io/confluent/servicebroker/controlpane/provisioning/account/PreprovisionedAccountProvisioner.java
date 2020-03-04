package io.confluent.servicebroker.controlpane.provisioning.account;

import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterCredentials;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

@Component
public class PreprovisionedAccountProvisioner extends AbstractAccountProvisioner {
	public PreprovisionedAccountProvisioner(ControlPaneConfiguration controlPaneConfiguration) {
		super(controlPaneConfiguration);
	}

	@Override
	public void assertAccountProviderConfiguration(AccountProviderConfiguration accountProviderConfiguration) {
		// Do Nothing.
	}

	public ClusterCredentials getCredentials(String principal,
			ClusterProviderConfiguration clusterProviderConfiguration) {
		return clusterProviderConfiguration.getCredentials().stream()
				.filter(configuredCredentials -> principal.equals(configuredCredentials.getName())).findAny()
				.orElseThrow(
						() -> new IllegalArgumentException("Could not find any credentials for " + principal + "."));
	}

	@Override
	public ProvisionerType getProvisionerType() {
		return ProvisionerType.PREPROVISIONED;
	}

	@Override
	public void removeCredentials(String principal, ClusterProviderConfiguration clusterProviderConfiguration) {
		// Nothing to do.
	}
}
