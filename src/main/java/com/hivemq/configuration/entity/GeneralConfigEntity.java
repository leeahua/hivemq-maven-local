package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;

public class GeneralConfigEntity {
    @XmlElement(name = "update-check-enabled", defaultValue = "true")
    private boolean updateCheckEnabled = true;

    public boolean isUpdateCheckEnabled() {
        return this.updateCheckEnabled;
    }
}
