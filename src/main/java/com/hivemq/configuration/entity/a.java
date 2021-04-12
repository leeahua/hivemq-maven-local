package com.hivemq.configuration.entity;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlJavaTypeAdapter(value = StringTrimAdapter.class, type = String.class)
abstract interface a {
}
