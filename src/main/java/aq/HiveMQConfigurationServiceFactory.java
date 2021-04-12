package aq;

import as.ConfigFileProvider;
import au.ConfigFile;
import au.ConfigurationReader;
import av.HiveMQConfigurationService;
import aw.GeneralConfigurationServiceImpl;
import aw.HiveMQConfigurationServiceImpl;
import aw.InternalConfigurationServiceImpl;
import aw.MqttConfigurationServiceImpl;
import aw.PersistenceConfigurationServiceImpl;
import aw.RestConfigurationServiceImpl;
import aw.SharedSubscriptionsConfigurationServiceImpl;
import aw.ThrottlingConfigurationServiceImpl;
import ax.ListenerConfigurationServiceImpl;
import cb1.EnvironmentVariableReplacer;
import com.hivemq.spi.config.SystemInformation;
import i.ClusterConfigurationService;

public class HiveMQConfigurationServiceFactory {
    public static HiveMQConfigurationService create(SystemInformation systemInformation) {
        HiveMQConfigurationServiceImpl hiveMQConfigurationService =
                new HiveMQConfigurationServiceImpl(
                        new GeneralConfigurationServiceImpl(),
                        new ListenerConfigurationServiceImpl(),
                        new MqttConfigurationServiceImpl(),
                        new InternalConfigurationServiceImpl(),
                        new ThrottlingConfigurationServiceImpl(),
                        new PersistenceConfigurationServiceImpl(),
                        new SharedSubscriptionsConfigurationServiceImpl(),
                        new RestConfigurationServiceImpl(),
                        new ClusterConfigurationService());
        ConfigFile configFile = ConfigFileProvider.get(systemInformation);
        ConfigurationReader configurationReader = new ConfigurationReader(configFile,
                hiveMQConfigurationService.generalConfiguration(),
                hiveMQConfigurationService.mqttConfiguration(),
                hiveMQConfigurationService.throttlingConfiguration(),
                hiveMQConfigurationService.listenerConfiguration(),
                hiveMQConfigurationService.internalConfiguration(),
                hiveMQConfigurationService.persistenceConfiguration(),
                hiveMQConfigurationService.restConfiguration(),
                hiveMQConfigurationService.sharedSubscriptionsConfiguration(),
                hiveMQConfigurationService.clusterConfiguration(),
                new EnvironmentVariableReplacer());
        configurationReader.read();
        return hiveMQConfigurationService;
    }
}
