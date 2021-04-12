package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name = "gc-type")
public enum GCTypeEntity {
    DELETE, RENAME;

    private GCTypeEntity() {
    }

    public String value() {
        return name();
    }

    public static GCTypeEntity fromValue(String paramString) {
        return valueOf(paramString.toUpperCase());
    }
}
