package aw;

import av.HiveMQConfigurationService;
import av.InternalConfigurationService;
import av.PersistenceConfigurationService;
import av.RestConfigurationService;
import av.SharedSubscriptionsConfigurationService;
import com.hivemq.spi.services.configuration.GeneralConfigurationService;
import com.hivemq.spi.services.configuration.MqttConfigurationService;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import com.hivemq.spi.services.configuration.listener.ListenerConfigurationService;
import i.ClusterConfigurationService;

public class HiveMQConfigurationServiceImpl implements HiveMQConfigurationService {
    private final GeneralConfigurationService generalConfigurationService;
    private final ListenerConfigurationService listenerConfigurationService;
    private final MqttConfigurationService mqttConfigurationService;
    private final InternalConfigurationService internalConfigurationService;
    private final ThrottlingConfigurationService throttlingConfigurationService;
    private final PersistenceConfigurationService persistenceConfigurationService;
    private final SharedSubscriptionsConfigurationService sharedSubscriptionsConfigurationService;
    private final RestConfigurationService restConfigurationService;
    private final ClusterConfigurationService clusterConfigurationService;

    public HiveMQConfigurationServiceImpl(GeneralConfigurationService generalConfigurationService,
                                          ListenerConfigurationService listenerConfigurationService,
                                          MqttConfigurationService mqttConfigurationService,
                                          InternalConfigurationService internalConfigurationService,
                                          ThrottlingConfigurationService throttlingConfigurationService,
                                          PersistenceConfigurationService persistenceConfigurationService,
                                          SharedSubscriptionsConfigurationService sharedSubscriptionsConfigurationService,
                                          RestConfigurationService restConfigurationService,
                                          ClusterConfigurationService clusterConfigurationService) {
        this.generalConfigurationService = generalConfigurationService;
        this.listenerConfigurationService = listenerConfigurationService;
        this.mqttConfigurationService = mqttConfigurationService;
        this.internalConfigurationService = internalConfigurationService;
        this.throttlingConfigurationService = throttlingConfigurationService;
        this.persistenceConfigurationService = persistenceConfigurationService;
        this.sharedSubscriptionsConfigurationService = sharedSubscriptionsConfigurationService;
        this.restConfigurationService = restConfigurationService;
        this.clusterConfigurationService = clusterConfigurationService;
    }

    public GeneralConfigurationService generalConfiguration() {
        return this.generalConfigurationService;
    }

    public ListenerConfigurationService listenerConfiguration() {
        return this.listenerConfigurationService;
    }

    public MqttConfigurationService mqttConfiguration() {
        return this.mqttConfigurationService;
    }

    public ThrottlingConfigurationService throttlingConfiguration() {
        return this.throttlingConfigurationService;
    }

    public InternalConfigurationService internalConfiguration() {
        return this.internalConfigurationService;
    }

    public PersistenceConfigurationService persistenceConfiguration() {
        return this.persistenceConfigurationService;
    }

    public SharedSubscriptionsConfigurationService sharedSubscriptionsConfiguration() {
        return this.sharedSubscriptionsConfigurationService;
    }

    public RestConfigurationService restConfiguration() {
        return this.restConfigurationService;
    }

    public ClusterConfigurationService clusterConfiguration() {
        return this.clusterConfigurationService;
    }
}
