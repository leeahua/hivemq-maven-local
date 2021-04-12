package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

public class SharedSubscriptionsEntity {
    @XmlElement(name = "shared-subscription")
    private List<String> sharedSubscriptions = new ArrayList();

    public List<String> getSharedSubscriptions() {
        return this.sharedSubscriptions;
    }
}
