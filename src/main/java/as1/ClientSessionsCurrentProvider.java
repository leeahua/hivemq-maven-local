package as1;

import ao1.ClientSessionsCurrent;
import bc1.ClientSessionLocalPersistence;
import com.codahale.metrics.MetricRegistry;
import com.hivemq.spi.metrics.HiveMQMetrics;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

public class ClientSessionsCurrentProvider implements Provider<ClientSessionsCurrent> {
    private final MetricRegistry metricRegistry;
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;

    @Inject
    public ClientSessionsCurrentProvider(MetricRegistry metricRegistry,
                                         ClientSessionLocalPersistence clientSessionLocalPersistence) {
        this.metricRegistry = metricRegistry;
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
    }

    @Singleton
    @Override
    public ClientSessionsCurrent get() {
        ClientSessionsCurrent clientSessionsCurrent = new ClientSessionsCurrent(this.clientSessionLocalPersistence);
        this.metricRegistry.register(HiveMQMetrics.CLIENT_SESSIONS_CURRENT.name(), clientSessionsCurrent);
        return clientSessionsCurrent;
    }
}
