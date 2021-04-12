package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class RetainedMessagesPersistenceConfigEntity
        extends PersistenceEntity {
    @XmlElement(name = "mode", defaultValue = "file")
    private PersistenceModeEntity persistenceMode = PersistenceModeEntity.FILE;

    public PersistenceModeEntity getPersistenceMode() {
        return this.persistenceMode;
    }
}
