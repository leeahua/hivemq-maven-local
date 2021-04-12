package com.hivemq.configuration.entity;

import com.google.common.collect.Lists;

import javax.xml.bind.annotation.XmlElements;
import java.util.List;

public class InternalConfigEntity {
    @XmlElements({@javax.xml.bind.annotation.XmlElement(name = "option", type = OptionEntity.class)})
    private List<OptionEntity> options = Lists.newArrayList();

    public List<OptionEntity> getOptions() {
        return this.options;
    }
}
