package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;

public abstract class ListenerEntity {
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

    void setPort(int paramInt) {
        this.port = paramInt;
    }

    void setBindAddress(String paramString) {
        this.bindAddress = paramString;
    }
}
