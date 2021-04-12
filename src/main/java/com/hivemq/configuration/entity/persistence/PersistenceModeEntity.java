package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

@XmlEnum
@XmlType(name = "mode")
public enum PersistenceModeEntity {
    IN_MEMORY, FILE;

    private PersistenceModeEntity() {
    }

    public String value() {
        return name();
    }

    public static PersistenceModeEntity fromValue(String paramString) {
        return valueOf(paramString.toUpperCase());
    }
}
