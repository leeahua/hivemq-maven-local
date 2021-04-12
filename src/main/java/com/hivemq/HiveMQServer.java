package com.hivemq;

import am1.StatisticsListener;
import aq.HiveMQConfigurationServiceFactory;
import ar.HiveMQSystemInformation;
import at1.MigrationType;
import at1.Migrations;
import av.HiveMQConfigurationService;
import b.ListenerStartResult;
import b.ListenerStartResultLogger;
import b.LogConfigurator;
import b.NettyServer;
import b.RecoverableExceptionHandler;
import c.Bootstrap;
import cc.PluginBrokerCallbackHandler;
import cl.PluginInformation;
import cl.PluginInformationStore;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Injector;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.configuration.entity.Listener;
import cw.UpdateChecker;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import i.ClusterJoiner;
import io.netty.channel.ChannelException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.DifferentConfigurationException;
import p.DuplicateOrInvalidLicenseException;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class HiveMQServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(HiveMQServer.class);
    private final NettyServer nettyServer;
    private final ClusterConfigurationService clusterConfigurationService;
    private final PluginBrokerCallbackHandler pluginBrokerCallbackHandler;
    private final PluginInformationStore pluginInformationStore;
    private final Provider<ClusterJoiner> clusterJoinerProvider;

    @Inject
    HiveMQServer(NettyServer nettyServer,
                 ClusterConfigurationService clusterConfigurationService,
                 PluginBrokerCallbackHandler pluginBrokerCallbackHandler,
                 PluginInformationStore pluginInformationStore,
                 Provider<ClusterJoiner> clusterJoinerProvider) {
        this.nettyServer = nettyServer;
        this.clusterConfigurationService = clusterConfigurationService;
        this.pluginBrokerCallbackHandler = pluginBrokerCallbackHandler;
        this.pluginInformationStore = pluginInformationStore;
        this.clusterJoinerProvider = clusterJoinerProvider;
    }

    public void start(Listener... listeners) throws InterruptedException, ExecutionException {
        try {
            this.nettyServer.start().sync();
        } catch (ChannelException e) {
        }
        fireOnBrokerStart();
        joinCluster();
        ListenableFuture<List<ListenerStartResult>> startFuture = this.nettyServer.startListeners(Arrays.asList(listeners));
        List<ListenerStartResult> startResults = startFuture.get();
        new ListenerStartResultLogger(startResults).log();
    }

    public void start() throws InterruptedException, ExecutionException {
        this.nettyServer.start().sync();
        fireOnBrokerStart();
        joinCluster();
        ListenableFuture<List<ListenerStartResult>> startFuture = this.nettyServer.startListeners();
        List<ListenerStartResult> startResults = startFuture.get();
        new ListenerStartResultLogger(startResults).log();
    }

    private void joinCluster() {
        if (!this.clusterConfigurationService.isEnabled()) {
            return;
        }
        try {
            ClusterJoiner clusterJoiner = this.clusterJoinerProvider.get();
            ListenableFuture<Void> future = clusterJoiner.join();
            future.get();
        } catch (Exception e) {
            if (e.getCause() instanceof DuplicateOrInvalidLicenseException) {
                LOGGER.error("Found duplicate or invalid license file in the cluster. Shutting down HiveMQ");
            } else if (e.getCause() instanceof DifferentConfigurationException) {
                LOGGER.error("The configuration of this HiveMQ instance is different form the other instances in the cluster. Shutting down HiveMQ");
            } else {
                LOGGER.error("Could not join cluster. Shutting down HiveMQ.", e);
            }
            if (e.getCause() instanceof UnrecoverableException) {
                throw ((UnrecoverableException) e.getCause());
            }
            throw new UnrecoverableException(false);
        }
    }

    private void fireOnBrokerStart() {
        LOGGER.trace("Calling all OnBrokerStart Callbacks");
        printPluginInformations();
        this.pluginBrokerCallbackHandler.onStart();
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        LOGGER.info("Starting HiveMQ Server");
        long startTime = System.nanoTime();
        LOGGER.trace("Initializing HiveMQ home directory");
        HiveMQSystemInformation systemInformation = new HiveMQSystemInformation(true);
        LOGGER.trace("Creating MetricRegistry");
        MetricRegistry metricRegistry = new MetricRegistry();
        metricRegistry.addListener(new StatisticsListener());
        LOGGER.trace("Initializing Logging");
        LogConfigurator.init(systemInformation.getConfigFolder(), metricRegistry);
        LOGGER.trace("Initializing Exception handlers");
        RecoverableExceptionHandler.init();
        LOGGER.trace("Initializing configuration");
        HiveMQConfigurationService hiveMQConfigurationService = HiveMQConfigurationServiceFactory.create(systemInformation);
        ClusterIdProducer clusterIdProducer = new ClusterIdProducer();
        if (hiveMQConfigurationService.clusterConfiguration().isEnabled()) {
            LOGGER.info("This node's cluster-ID is {}", clusterIdProducer.get());
        }
        LOGGER.trace("Checking for migrations");
        Map<MigrationType, Set<String>> neededMigrations = Migrations.getNeededMigrations(systemInformation);
        Injector injector = null;
        if (neededMigrations.size() > 0) {
            LOGGER.warn("HiveMQ has been updated, migrating persistent data to new version !");
            neededMigrations.keySet().forEach(type -> LOGGER.debug("{} needs to be migrated", type));
            injector = Bootstrap.createInjector(systemInformation, hiveMQConfigurationService, clusterIdProducer);
            Migrations.start(injector, neededMigrations);
        }
        Migrations.finish(systemInformation, hiveMQConfigurationService);
        LOGGER.trace("Initializing Guice");
        injector = Bootstrap.createInjector(systemInformation, metricRegistry, hiveMQConfigurationService, clusterIdProducer, injector);
        HiveMQServer server = injector.getInstance(HiveMQServer.class);
        server.start();
        LogConfigurator.addXodusLogModificator();
        LOGGER.info("Started HiveMQ in {}ms", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime));
        UpdateChecker updateChecker = injector.getInstance(UpdateChecker.class);
        updateChecker.start();
    }

    private void printPluginInformations() {
        Set<PluginInformation> pluginInformations = this.pluginInformationStore.getPluginInformations();
        pluginInformations.forEach(pluginInformation ->
                LOGGER.info("Loaded Plugin {} - v{}", pluginInformation.getName(), pluginInformation.getVersion())
        );
    }
}
