package io.confluent.servicebroker.controlpane.zookeeper;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;
import javax.security.auth.login.Configuration;

import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ZookeeperConfiguration;

@Component
public class ZookeeperLoginManager extends Configuration {
	private final Map<String, AppConfigurationEntry> entries;

	public ZookeeperLoginManager(ControlPaneConfiguration controlPaneConfiguration) {
		this.entries = controlPaneConfiguration.getClusterProviders().stream()

				.filter(clusterProviderConfiguration -> clusterProviderConfiguration.getZookeeper() != null
						&& clusterProviderConfiguration.getZookeeper().getLoginModule() != null)

				.collect(Collectors.toMap(
						clusterProviderConfiguration -> clusterProviderConfiguration.getProviderName(), this::map));

		Configuration.setConfiguration(this);
	}

	@Override
	public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
		return entries.entrySet().stream().filter(entry -> entry.getKey().equals(name))

				.map(Entry::getValue)

				.toArray(AppConfigurationEntry[]::new);
	}

	private AppConfigurationEntry map(ClusterProviderConfiguration clusterProviderConfiguration) {
		validateZookeeperConfiguration(clusterProviderConfiguration.getProviderName(),
				clusterProviderConfiguration.getZookeeper());

		return new AppConfigurationEntry(clusterProviderConfiguration.getZookeeper().getLoginModule().getName(),
				getControlFlag(clusterProviderConfiguration.getZookeeper().getControlFlag()),
				clusterProviderConfiguration.getZookeeper().getOptions());
	}

	private LoginModuleControlFlag getControlFlag(String controlFlag) {
		if ("required".equals(controlFlag)) {
			return LoginModuleControlFlag.REQUIRED;
		} else if ("optional".equals(controlFlag)) {
			return LoginModuleControlFlag.OPTIONAL;
		} else if ("requisite".equals(controlFlag)) {
			return LoginModuleControlFlag.REQUISITE;
		} else if ("sufficient".equals(controlFlag)) {
			return LoginModuleControlFlag.SUFFICIENT;
		} else {
			throw new IllegalArgumentException(
					controlFlag + " is not a valid control flag for zookeeper login module.");
		}
	}

	private void validateZookeeperConfiguration(String clusterProviderName,
			ZookeeperConfiguration zookeeperConfiguration) {
		if (zookeeperConfiguration.getControlFlag() == null) {
			throw new IllegalArgumentException(
					"Control flag is undefined on Zookeeper login module for cluster provider " + clusterProviderName);
		}

		if (zookeeperConfiguration.getOptions() == null) {
			throw new IllegalArgumentException(
					"Options are undefined on the Zookeeper login module for cluster provider " + clusterProviderName);
		}
	}
}
