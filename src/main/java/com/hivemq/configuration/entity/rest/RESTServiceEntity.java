package com.hivemq.configuration.entity.rest;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {})
public class RESTServiceEntity {
    @XmlElementWrapper(name = "listeners", required = true)
    @XmlElements({@XmlElement(name = "http-listener", type = HttpRestListenerEntity.class)})
    private List<RestListenerEntity> listeners = new ArrayList();
    @XmlElement(name = "servlet-path", defaultValue = "/servlet")
    private String servletPath = "/servlet";
    @XmlElement(name = "jax-rs-path", defaultValue = "/*")
    private String jaxRsPath = "/*";

    public List<RestListenerEntity> getListeners() {
        return this.listeners;
    }

    public String getServletPath() {
        return this.servletPath;
    }

    public String getJaxRsPath() {
        return this.jaxRsPath;
    }
}
