package cf;

import aj.ClusterFutures;
import ap.ShutdownRegistry;
import ca.CallableTask;
import ca.CallbackExecutor;
import ca.RunnableTask;
import com.beust.jcommander.internal.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.cluster.ClusterDiscoveryCallback;
import com.hivemq.spi.callback.cluster.ClusterNodeAddress;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import k.ClusterDiscovery;
import l.PluginDiscovery;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.jgroups.Event;
import org.jgroups.JChannel;
import org.jgroups.PhysicalAddress;
import org.jgroups.stack.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import s.Cluster;
import t.ClusterConnection;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CacheScoped
public class ClusterPluginDiscoveryService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPluginDiscoveryService.class);
    private final CallbackRegistry callbackRegistry;
    private final CallbackExecutor callbackExecutor;
    private final ListeningScheduledExecutorService clusterScheduledExecutorService;
    private final ClusterConnection clusterConnection;
    private final ClusterIdProducer clusterIdProducer;
    private final ShutdownRegistry shutdownRegistry;
    private final ClusterConfigurationService clusterConfigurationService;
    private List<ClusterNodeAddress> nodeAddresses;
    private final Set<String> plugins = new ConcurrentHashSet<>();

    @Inject
    public ClusterPluginDiscoveryService(CallbackRegistry callbackRegistry,
                                         CallbackExecutor callbackExecutor,
                                         @Cluster ListeningScheduledExecutorService clusterScheduledExecutorService,
                                         ClusterConnection clusterConnection,
                                         ClusterIdProducer clusterIdProducer,
                                         ShutdownRegistry shutdownRegistry,
                                         ClusterConfigurationService clusterConfigurationService) {
        this.callbackRegistry = callbackRegistry;
        this.callbackExecutor = callbackExecutor;
        this.clusterScheduledExecutorService = clusterScheduledExecutorService;
        this.clusterConnection = clusterConnection;
        this.clusterIdProducer = clusterIdProducer;
        this.shutdownRegistry = shutdownRegistry;
        this.clusterConfigurationService = clusterConfigurationService;
    }

    @PostConstruct
    void init() {
        reload();
    }


    private Set<ListenableFuture<?>> initPlugins(List<ClusterDiscoveryCallback> callbacks) {
        ClusterNodeAddress nodeAddress = getNodeAddress();
        Set<ListenableFuture<?>> futures = new ConcurrentHashSet<>();
        callbacks.forEach(callback -> {
            registerShutdown(callback);
            ListenableFuture future = this.callbackExecutor.submit(new RunnableTask() {

                @Override
                public void run() {
                    try {
                        callback.init(clusterIdProducer.get(), nodeAddress);
                    } catch (Exception e) {
                        LOGGER.error("Uncaught exception in plugin", e);
                    }
                }

                @Override
                public Class callbackType() {
                    return callback.getClass();
                }
            });
            futures.add(future);
        });
        return futures;
    }

    @NotNull
    private ClusterNodeAddress getNodeAddress() {
        JChannel jChannel = this.clusterConnection.getJChannel();
        PhysicalAddress physicalAddress = (PhysicalAddress) jChannel.down(new Event(Event.GET_PHYSICAL_ADDRESS, jChannel.getAddress()));
        if (!(physicalAddress instanceof IpAddress)) {
            throw new IllegalStateException("Address must be an IpAddress");
        }
        IpAddress ipAddress = (IpAddress) physicalAddress;
        return new ClusterNodeAddress(ipAddress.getIpAddress().getHostName(), ipAddress.getPort());
    }

    private void reload() {
        long interval = getInterval();
        if (interval < 1L) {
            LOGGER.debug("Cluster plugin discovery reloading disabled");
            return;
        }
        this.clusterScheduledExecutorService.scheduleAtFixedRate(() -> {
            try {
                List<ClusterDiscoveryCallback> callbacks = getCallbacks();
                addPlugins(callbacks);
                ListenableFuture<List<ClusterNodeAddress>> fetchNodeAddressFuture = fetchNodeAddresses(callbacks);
                ClusterFutures.addCallback(fetchNodeAddressFuture, new FutureCallback<List<ClusterNodeAddress>>() {

                    @Override
                    public void onSuccess(@Nullable List<ClusterNodeAddress> result) {
                        if (result == null) {
                            return;
                        }
                        nodeAddresses = result;
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOGGER.error("Not able to update cluster nodes via plugin system: {}", t.getMessage());
                        LOGGER.debug("Original exception", t);
                    }
                });
            } catch (Exception localException) {
                LOGGER.error("Not able to update cluster nodes via plugin system: {}", localException.getMessage());
                LOGGER.debug("Original exception", localException);
            }
        }, interval, interval, TimeUnit.SECONDS);
    }

    private long getInterval() {
        ClusterDiscovery discovery = this.clusterConfigurationService.getDiscovery();
        if (discovery instanceof PluginDiscovery) {
            return ((PluginDiscovery) discovery).getInterval();
        }
        throw new IllegalStateException("Cluster discovery must be of type plugin");
    }

    private void registerShutdown(ClusterDiscoveryCallback discoveryCallback) {
        this.shutdownRegistry.register(
                new PluginClusterDiscoveryShutdown(this.callbackExecutor, discoveryCallback));
    }

    public synchronized List<ClusterNodeAddress> getClusterNodeAddresses() {
        List<ClusterDiscoveryCallback> callbacks = getCallbacks();
        addPlugins(callbacks);
        if (this.nodeAddresses != null) {
            return this.nodeAddresses;
        }
        try {
            this.nodeAddresses = fetchNodeAddresses(callbacks).get();
            return this.nodeAddresses;
        } catch (InterruptedException e) {
            LOGGER.debug("Fetching cluster node addresses interrupted");
            return Collections.emptyList();
        } catch (ExecutionException e) {
            LOGGER.error("Not able to fetch cluster node addresses from plugin: {}", e.getMessage());
            LOGGER.debug("Original exception", e);
            return Collections.emptyList();
        }
    }

    private void addPlugins(List<ClusterDiscoveryCallback> callbacks) {
        List<ClusterDiscoveryCallback> validCallbacks = callbacks.stream()
                .filter(callback -> !this.plugins.contains(callback.getClass().getCanonicalName()))
                .collect(Collectors.toList());

        if (validCallbacks == null || validCallbacks.size() < 1) {
            return;
        }
        Set<ListenableFuture<?>> startFutures = initPlugins(validCallbacks);
        validCallbacks.forEach(callback ->
                this.plugins.add(callback.getClass().getCanonicalName()));
        try {
            ListenableFuture<List<Object>> future = Futures.allAsList(startFutures);
            future.get();
        } catch (InterruptedException e) {
            LOGGER.debug("Waiting for discovery plugin init interrupted");
        } catch (ExecutionException e) {
            LOGGER.error("Error at discovery plugin init: {}", e.getMessage());
            LOGGER.debug("Original exception", e);
        }
    }


    private ListenableFuture<List<ClusterNodeAddress>> fetchNodeAddresses(List<ClusterDiscoveryCallback> callbacks) {
        LOGGER.trace("Fetching cluster node addresses from plugins");
        if (callbacks == null || callbacks.size() < 1) {
            LOGGER.warn("Plugin cluster discovery is configured, but no ClusterDiscoveryCallback is registered");
            return Futures.immediateFuture(Collections.emptyList());
        }
        SettableFuture<List<ClusterNodeAddress>> settableFuture = SettableFuture.create();
        Set<ListenableFuture<List<ClusterNodeAddress>>> futures = new ConcurrentHashSet<>();
        callbacks.forEach(callback -> {
            ListenableFuture<ListenableFuture<List<ClusterNodeAddress>>> getNodeAddressFuture = this.callbackExecutor.submit(new Task(callback));
            futures.add(ClusterFutures.setFuture(getNodeAddressFuture));
        });
        ListenableFuture<List<List<ClusterNodeAddress>>> future = Futures.allAsList(futures);
        ClusterFutures.addCallback(future, new FutureCallback<List<List<ClusterNodeAddress>>>() {

            @Override
            public void onSuccess(@Nullable List<List<ClusterNodeAddress>> result) {
                if (result == null) {
                    settableFuture.set(Collections.emptyList());
                    return;
                }
                Set<ClusterNodeAddress> clusterNodeAddresses = result.stream()
                        .filter(Objects::nonNull)
                        .flatMap(List::stream)
                        .collect(Collectors.toSet());
                settableFuture.set(Lists.newArrayList(clusterNodeAddresses));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }


    public List<ClusterDiscoveryCallback> getCallbacks() {
        return this.callbackRegistry.getCallbacks(ClusterDiscoveryCallback.class);
    }

    public static class Task
            implements CallableTask<ListenableFuture<List<ClusterNodeAddress>>> {
        private final ClusterDiscoveryCallback callback;

        public Task(ClusterDiscoveryCallback callback) {
            this.callback = callback;
        }

        @NotNull
        public Class callbackType() {
            return this.callback.getClass();
        }

        @Override
        public ListenableFuture<List<ClusterNodeAddress>> call() throws Exception {
            return this.callback.getNodeAddresses();
        }
    }
}
