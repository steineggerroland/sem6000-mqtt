package org.magcode.sem6000;

import org.magcode.sem6000.connector.Connector;
import org.magcode.sem6000.connectorv2.ConnectorV2;

public class Sem6000Config {
	private String mac;
	private String pin;
	private ConnectorV2 connector;
	private String name;

	public Sem6000Config(String mac, String pin, String name) {
		this.mac = mac;
		this.pin = pin;
		this.name = name;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public ConnectorV2 getConnector() {
		return connector;
	}

	public void setConnector(ConnectorV2 connector) {
		this.connector = connector;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
