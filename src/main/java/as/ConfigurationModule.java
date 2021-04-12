package as;

import at.AnnotationMatcher;
import at.MethodValidateInterceptor;
import av.HiveMQConfigurationService;
import av.InternalConfigurationService;
import av.PersistenceConfigurationService;
import av.RestConfigurationService;
import av.SharedSubscriptionsConfigurationService;
import c.BaseModule;
import com.google.inject.matcher.Matchers;
import com.hivemq.spi.services.ConfigurationService;
import com.hivemq.spi.services.configuration.GeneralConfigurationService;
import com.hivemq.spi.services.configuration.MqttConfigurationService;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import com.hivemq.spi.services.configuration.listener.ListenerConfigurationService;
import com.hivemq.spi.services.configuration.validation.annotation.Validate;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;

public class ConfigurationModule extends BaseModule<ConfigurationModule> {
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final ClusterIdProducer clusterIdProducer;

    public ConfigurationModule(HiveMQConfigurationService hiveMQConfigurationService,
                               ClusterIdProducer clusterIdProducer) {
        super(ConfigurationModule.class);
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.clusterIdProducer = clusterIdProducer;
    }

    protected void configure() {
        bind(ClusterIdProducer.class).toInstance(this.clusterIdProducer);
        bind(ListenerConfigurationService.class).toInstance(this.hiveMQConfigurationService.listenerConfiguration());
        bind(MqttConfigurationService.class).toInstance(this.hiveMQConfigurationService.mqttConfiguration());
        bind(ThrottlingConfigurationService.class).toInstance(this.hiveMQConfigurationService.throttlingConfiguration());
        bind(InternalConfigurationService.class).toInstance(this.hiveMQConfigurationService.internalConfiguration());
        bind(PersistenceConfigurationService.class).toInstance(this.hiveMQConfigurationService.persistenceConfiguration());
        bind(ConfigurationService.class).toInstance(this.hiveMQConfigurationService);
        bind(HiveMQConfigurationService.class).toInstance(this.hiveMQConfigurationService);
        bind(GeneralConfigurationService.class).toInstance(this.hiveMQConfigurationService.generalConfiguration());
        bind(RestConfigurationService.class).toInstance(this.hiveMQConfigurationService.restConfiguration());
        bind(SharedSubscriptionsConfigurationService.class).toInstance(this.hiveMQConfigurationService.sharedSubscriptionsConfiguration());
        bind(ClusterConfigurationService.class).toInstance(this.hiveMQConfigurationService.clusterConfiguration());
        bindInterceptor(Matchers.any(), new AnnotationMatcher(Validate.class), new MethodValidateInterceptor());
    }
}
