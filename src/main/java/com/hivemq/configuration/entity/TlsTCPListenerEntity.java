package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;

public class TlsTCPListenerEntity
        extends ListenerEntity {
    @XmlElement(name = "tls", required = true)
    private TLSEntity tls = new TLSEntity();

    public TLSEntity getTls() {
        return this.tls;
    }
}
