package com.hivemq.configuration.entity.rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public abstract class RestListenerEntity {
    @XmlElement(name = "name", required = true)
    private String name;
    @XmlElement(name = "port", required = true)
    private int port;
    @XmlElement(name = "bind-address", required = true)
    private String bindAddress = "0.0.0.0";

    public int getPort() {
        return this.port;
    }

    public String getBindAddress() {
        return this.bindAddress;
    }

    public String getName() {
        return this.name;
    }
}
