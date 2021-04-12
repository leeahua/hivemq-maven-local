package c;

import a.MethodCacheModule;
import af1.LicensingModule;
import ak1.LifecycleModule;
import ar1.MetricModule;
import as.ConfigurationModule;
import au1.BridgeModule;
import av.HiveMQConfigurationService;
import ay.DiagnosticModule;
import ba1.PersistenceModule;
import bo1.PluginBootstrap;
import bt.PluginCallbackModule;
import cb.ServiceModule;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.hivemq.spi.PluginModule;
import com.hivemq.spi.config.SystemInformation;
import cv.TrafficShapingModule;
import cz.UpdateModule;
import d.ScopeModule;
import g.NettyModule;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import r.ClusterModule;

import java.util.List;

public class Bootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static Injector createInjector(SystemInformation systemInformation,
                                          MetricRegistry metricRegistry,
                                          HiveMQConfigurationService hiveMQConfigurationService,
                                          ClusterIdProducer clusterIdProducer,
                                          Injector injector) {
        if (!Boolean.parseBoolean(System.getProperty("diagnosticMode"))) {
            LOGGER.trace("Turning Guice stack traces off");
            System.setProperty("guice_include_stack_traces", "OFF");
        }
        List<PluginModule> pluginModules = new PluginBootstrap().create(systemInformation.getPluginFolder());
        ImmutableList.Builder<AbstractModule> builder = ImmutableList.builder();
        builder.add(
                new SystemInformationModule(systemInformation),
                new ScopeModule(),
                new LifecycleModule(),
                new ConfigurationModule(hiveMQConfigurationService, clusterIdProducer),
                new NettyModule(),
                new InternalModule(),
                new PluginCallbackModule(),
                new MethodCacheModule(),
                new PersistenceModule(injector),
                new MetricModule(metricRegistry),
                new TrafficShapingModule(),
                new ClusterModule(),
                new ServiceModule(pluginModules),
                new LicensingModule(),
                new UpdateModule(),
                new DiagnosticModule());
        builder.addAll(pluginModules);
        return Guice.createInjector(Stage.PRODUCTION, builder.build());
    }

    public static Injector createInjector(SystemInformation systemInformation,
                                          HiveMQConfigurationService hiveMQConfigurationService,
                                          ClusterIdProducer clusterIdProducer) {
        ImmutableList.Builder<AbstractModule> builder = ImmutableList.builder();
        builder.add(
                new SystemInformationModule(systemInformation),
                new ConfigurationModule(hiveMQConfigurationService, clusterIdProducer),
                new BridgeModule(),
                new ScopeModule(),
                new LifecycleModule());
        return Guice.createInjector(Stage.PRODUCTION, builder.build());
    }
}
