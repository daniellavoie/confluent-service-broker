package io.confluent.servicebroker.controlpane.zookeeper;

import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry.LoginModuleControlFlag;

import io.confluent.servicebroker.controlpane.jaas.LoginModule;

public class LoginModuleConfig {
	private final String name;
	private final LoginModule loginModule;
	private final LoginModuleControlFlag controlFlag;
	private final Map<String, String> options;

	public LoginModuleConfig(String name, LoginModule loginModule, LoginModuleControlFlag controlFlag,
			Map<String, String> options) {
		this.name = name;
		this.loginModule = loginModule;
		this.controlFlag = controlFlag;
		this.options = options;
	}

	public String getName() {
		return name;
	}

	public LoginModule getLoginModule() {
		return loginModule;
	}

	public LoginModuleControlFlag getControlFlag() {
		return controlFlag;
	}

	public Map<String, String> getOptions() {
		return options;
	}

}
