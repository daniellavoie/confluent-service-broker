package io.confluent.servicebroker.controlpane.model;

import java.util.Map;

import io.confluent.servicebroker.controlpane.jaas.LoginModule;

public class ZookeeperConfiguration {
	private String connectString;
	private LoginModule loginModule;
	private String controlFlag;
	private Map<String, String> options;
	private int sessionTimeout = 2000000;

	public String getConnectString() {
		return connectString;
	}

	public void setConnectString(String connectString) {
		this.connectString = connectString;
	}

	public LoginModule getLoginModule() {
		return loginModule;
	}

	public void setLoginModule(LoginModule loginModule) {
		this.loginModule = loginModule;
	}

	public String getControlFlag() {
		return controlFlag;
	}

	public void setControlFlag(String controlFlag) {
		this.controlFlag = controlFlag;
	}

	public Map<String, String> getOptions() {
		return options;
	}

	public void setOptions(Map<String, String> options) {
		this.options = options;
	}

	public int getSessionTimeout() {
		return sessionTimeout;
	}

	public void setSessionTimeout(int sessionTimeout) {
		this.sessionTimeout = sessionTimeout;
	}
}
