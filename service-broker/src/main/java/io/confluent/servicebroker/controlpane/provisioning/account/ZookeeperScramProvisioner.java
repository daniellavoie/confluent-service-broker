package io.confluent.servicebroker.controlpane.provisioning.account;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.kafka.common.security.scram.internals.ScramCredentialUtils;
import org.apache.kafka.common.security.scram.internals.ScramFormatter;
import org.apache.kafka.common.security.scram.internals.ScramMechanism;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.client.ZKClientConfig;
import org.apache.zookeeper.data.Stat;
import org.springframework.stereotype.Component;

import io.confluent.servicebroker.controlpane.config.ControlPaneConfiguration;
import io.confluent.servicebroker.controlpane.model.AccountProviderConfiguration;
import io.confluent.servicebroker.controlpane.model.ClusterCredentials;
import io.confluent.servicebroker.controlpane.model.ClusterProviderConfiguration;

@Component
public class ZookeeperScramProvisioner extends AbstractAccountProvisioner {

	public ZookeeperScramProvisioner(ControlPaneConfiguration controlPaneConfiguration) {
		super(controlPaneConfiguration);

		// TODO - Asserts zookeeper configurations.
	}

	@Override
	public void assertAccountProviderConfiguration(AccountProviderConfiguration accountProviderConfiguration) {
		// Nothing to do.
	}

	@Override
	protected ClusterCredentials getCredentials(String principal,
			ClusterProviderConfiguration clusterProviderConfiguration) {

		ClusterCredentials clusterCredentials = new ClusterCredentials();

		clusterCredentials.setUsername(principal.replace("/", "-"));
		clusterCredentials.setPassword(randomString(30));

		runZookeeperCommand(clusterProviderConfiguration, zookeeper -> {
			try {

				String path = "/config/users/" + clusterCredentials.getUsername();
				Stat nodeStat = zookeeper.exists(path, false);
				String data = "{\"version\":1,\"config\":{\"SCRAM-SHA-512\":\""
						+ ScramCredentialUtils.credentialToString(new ScramFormatter(ScramMechanism.SCRAM_SHA_512)
								.generateCredential(clusterCredentials.getPassword(), 4096))
						+ "\"}}";
				if (nodeStat != null) {
					zookeeper.setData(path, data.getBytes(), nodeStat.getVersion());
				} else {
					zookeeper.create(path, data.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				}

				String changeData = "{\"version\": 2, \"entity_path\":\"users/" + clusterCredentials.getUsername()
						+ "\"}";

				zookeeper.create("/config/changes/config_change_", changeData.getBytes(), ZooDefs.Ids.OPEN_ACL_UNSAFE,
						CreateMode.PERSISTENT_SEQUENTIAL);
			} catch (KeeperException | InterruptedException | NoSuchAlgorithmException e) {
				throw new RuntimeException(e);
			}
		});

		System.out.println(clusterCredentials.getPassword());

		return clusterCredentials;
	}

	@Override
	public ProvisionerType getProvisionerType() {
		return ProvisionerType.ZOOKEEPER_SCRAM;
	}

	private ZooKeeper getZooKeeperConnection(ClusterProviderConfiguration clusterProviderConfiguration)
			throws IOException, InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);

		ZKClientConfig zkClientConfig = new ZKClientConfig();
		zkClientConfig.setProperty(ZKClientConfig.LOGIN_CONTEXT_NAME_KEY,
				clusterProviderConfiguration.getProviderName());

		ZooKeeper zooKeeper = new ZooKeeper(clusterProviderConfiguration.getZookeeper().getConnectString(),
				clusterProviderConfiguration.getZookeeper().getSessionTimeout(), watchedEvent -> {
					if (watchedEvent.getState() == KeeperState.SyncConnected) {
						latch.countDown();
					}
				}, zkClientConfig);

		try {
			if (!latch.await(clusterProviderConfiguration.getZookeeper().getSessionTimeout(), TimeUnit.MILLISECONDS)) {
				throw new RuntimeException("Failed to connect to Zookeeper "
						+ clusterProviderConfiguration.getZookeeper().getConnectString());
			}

			return zooKeeper;
		} catch (InterruptedException ex) {
			zooKeeper.close();

			throw ex;
		}
	}

	@Override
	public void removeCredentials(String principal, ClusterProviderConfiguration clusterProviderConfiguration) {
		runZookeeperCommand(clusterProviderConfiguration, zookeeper -> {
			try {
				String path = "/config/users/" + principal.replace("/", "-");
				Stat nodeStat = zookeeper.exists(path, false);
				if (nodeStat != null) {
					zookeeper.delete(path, nodeStat.getVersion());
				}
			} catch (KeeperException | InterruptedException e) {
				throw new RuntimeException(e);
			}
		});
	}

	private void runZookeeperCommand(ClusterProviderConfiguration clusterProviderConfiguration,
			Consumer<ZooKeeper> command) {
		try (ZooKeeper zookeeper = getZooKeeperConnection(clusterProviderConfiguration)) {
			command.accept(zookeeper);
		} catch (InterruptedException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
