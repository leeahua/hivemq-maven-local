package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.ArrayList;
import java.util.List;

public class TlsWebsocketListenerEntity
        extends ListenerEntity {
    @XmlElement(required = true)
    private String path = "/mqtt";
    @XmlElementWrapper(name = "subprotocols")
    @XmlElement(name = "subprotocol")
    private List<String> subprotocols = defaultProtocols();
    @XmlElement(name = "allow-extensions", defaultValue = "false")
    private boolean allowExtensions = false;
    @XmlElement(name = "tls", required = true)
    private TLSEntity tls = new TLSEntity();

    public TLSEntity getTls() {
        return this.tls;
    }

    public String getPath() {
        return this.path;
    }

    public List<String> getSubprotocols() {
        return this.subprotocols;
    }

    public boolean isAllowExtensions() {
        return this.allowExtensions;
    }

    private List<String> defaultProtocols() {
        ArrayList localArrayList = new ArrayList();
        localArrayList.add("mqttv3.1");
        localArrayList.add("mqtt");
        return localArrayList;
    }
}
