package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name = "clientAuthenticationMode")
public enum ClientAuthenticationModeEntity {
    OPTIONAL, REQUIRED, NONE;

    private ClientAuthenticationModeEntity() {
    }

    public String value() {
        return name();
    }

    public static ClientAuthenticationModeEntity fromValue(String paramString) {
        return valueOf(paramString.toUpperCase());
    }
}
