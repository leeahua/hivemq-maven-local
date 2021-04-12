package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class StringTrimAdapter
        extends XmlAdapter<String, String> {
    public String unmarshal(String paramString) {
        if (paramString == null) {
            return null;
        }
        return paramString.trim();
    }

    public String marshal(String paramString) {
        if (paramString == null) {
            return null;
        }
        return paramString.trim();
    }
}
