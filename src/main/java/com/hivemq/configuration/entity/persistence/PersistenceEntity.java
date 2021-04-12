package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType
public abstract class PersistenceEntity {
    @XmlElement(name = "file-persistence-configuration")
    private PersistenceConfigConfigurationEntity configuration = new PersistenceConfigConfigurationEntity();

    public PersistenceConfigConfigurationEntity getConfiguration() {
        return this.configuration;
    }
}
