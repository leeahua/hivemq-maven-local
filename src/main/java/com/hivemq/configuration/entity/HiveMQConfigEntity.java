package com.hivemq.configuration.entity;

import com.hivemq.configuration.entity.persistence.PersistenceConfigEntity;
import com.hivemq.configuration.entity.rest.RESTServiceEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

@XmlType(propOrder = {})
@XmlRootElement(name = "hivemq")
public class HiveMQConfigEntity {
    @XmlElementWrapper(name = "listeners", required = true)
    @XmlElements({@XmlElement(name = "tcp-listener", type = TCPListenerEntity.class), @XmlElement(name = "websocket-listener", type = WebsocketListenerEntity.class), @XmlElement(name = "tls-tcp-listener", type = TlsTCPListenerEntity.class), @XmlElement(name = "tls-websocket-listener", type = TlsWebsocketListenerEntity.class)})
    private List<ListenerEntity> listeners = new ArrayList();
    @XmlElement(name = "mqtt")
    private MqttConfigEntity mqttConfigEntity = new MqttConfigEntity();
    @XmlElement(name = "throttling")
    private ThrottlingEntity throttling = new ThrottlingEntity();
    @XmlElement(name = "general")
    private GeneralConfigEntity general = new GeneralConfigEntity();
    @XmlElement(name = "internal")
    private InternalConfigEntity internal = new InternalConfigEntity();
    @XmlElement(name = "shared-subscriptions")
    private SharedSubscriptionsEntity sharedSubscriptions = new SharedSubscriptionsEntity();
    @XmlElement(name = "persistence")
    private PersistenceConfigEntity persistence = new PersistenceConfigEntity();
    @XmlElement(name = "rest-service")
    private RESTServiceEntity restService = new RESTServiceEntity();
    @XmlElement(name = "cluster")
    private ClusterConfigEntity cluster = new ClusterConfigEntity();

    public List<ListenerEntity> getListeners() {
        return this.listeners;
    }

    public MqttConfigEntity getMqtt() {
        return this.mqttConfigEntity;
    }

    public ThrottlingEntity getThrottling() {
        return this.throttling;
    }

    public GeneralConfigEntity getGeneral() {
        return this.general;
    }

    public InternalConfigEntity getInternal() {
        return this.internal;
    }

    public SharedSubscriptionsEntity getSharedSubscriptions() {
        return this.sharedSubscriptions;
    }

    public PersistenceConfigEntity getPersistence() {
        return this.persistence;
    }

    public RESTServiceEntity getRestService() {
        return this.restService;
    }

    public ClusterConfigEntity getCluster() {
        return this.cluster;
    }
}
