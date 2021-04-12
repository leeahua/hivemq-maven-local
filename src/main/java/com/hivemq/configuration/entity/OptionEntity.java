package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;

public class OptionEntity {
    @XmlElement(name = "key", required = true)
    private String key;
    @XmlElement(name = "value", required = true)
    private String value;

    public String getKey() {
        return this.key;
    }

    public String getValue() {
        return this.value;
    }
}
