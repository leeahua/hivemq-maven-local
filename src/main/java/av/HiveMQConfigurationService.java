package av;

import com.hivemq.spi.services.ConfigurationService;
import i.ClusterConfigurationService;

public interface HiveMQConfigurationService extends ConfigurationService {
    InternalConfigurationService internalConfiguration();

    PersistenceConfigurationService persistenceConfiguration();

    SharedSubscriptionsConfigurationService sharedSubscriptionsConfiguration();

    RestConfigurationService restConfiguration();

    ClusterConfigurationService clusterConfiguration();
}
