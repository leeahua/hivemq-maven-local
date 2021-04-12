package com.hivemq.configuration.entity.persistence;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class PersistenceConfigConfigurationEntity {
    @XmlElement(name = "jmx-enabled", defaultValue = "true")
    private boolean jmxEnabled = true;
    @XmlElement(name = "garbage-collection-type", defaultValue = "delete")
    private GCTypeEntity gcTypeEntity = GCTypeEntity.DELETE;
    @XmlElement(name = "garbage-collection-deletion-delay", defaultValue = "60000")
    int gcDeletionDelay = 60000;
    @XmlElement(name = "garbage-collection-run-period", defaultValue = "30000")
    int gcRunPeriod = 30000;
    @XmlElement(name = "garbage-collection-files-interval", defaultValue = "1")
    int gcFilesInterval = 1;
    @XmlElement(name = "garbage-collection-min-file-age", defaultValue = "2")
    int gcMinAge = 2;
    @XmlElement(name = "sync-period", defaultValue = "1000")
    int syncPeriod = 1000;
    @XmlElement(name = "durable-writes", defaultValue = "false")
    boolean durableWrites = false;

    public boolean isJmxEnabled() {
        return this.jmxEnabled;
    }

    public GCTypeEntity getGcTypeEntity() {
        return this.gcTypeEntity;
    }

    public int getGcDeletionDelay() {
        return this.gcDeletionDelay;
    }

    public int getGcRunPeriod() {
        return this.gcRunPeriod;
    }

    public int getGcFilesInterval() {
        return this.gcFilesInterval;
    }

    public int getGcMinAge() {
        return this.gcMinAge;
    }

    public int getSyncPeriod() {
        return this.syncPeriod;
    }

    public boolean isDurableWrites() {
        return this.durableWrites;
    }
}
