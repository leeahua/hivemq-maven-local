package co;

import aj.ClusterFutures;
import bl1.ChannelPersistence;
import cb1.ChannelUtils;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.services.AsyncClientService;
import com.hivemq.spi.services.PluginExecutorService;
import d.CacheScoped;
import i.ClusterConfigurationService;
import io.netty.channel.Channel;
import k1.ClusterCallbackFactoryImpl;
import t.ClusterConnection;
import t1.ClientDataRequest;
import t1.ConnectedClientsRequest;
import t1.DisconnectedClientsRequest;
import v.ClientSession;
import v.ClientSessionClusterPersistence;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

@CacheScoped
public class AsyncClientServiceImpl implements AsyncClientService {
    private final ChannelPersistence channelPersistence;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ClusterConfigurationService clusterConfigurationService;
    private final ClusterConnection clusterConnection;
    private final PluginExecutorService pluginExecutorService;

    @Inject
    public AsyncClientServiceImpl(ChannelPersistence channelPersistence,
                                  ClientSessionClusterPersistence clientSessionClusterPersistence,
                                  ClusterConfigurationService clusterConfigurationService,
                                  ClusterConnection clusterConnection,
                                  PluginExecutorService pluginExecutorService) {
        this.channelPersistence = channelPersistence;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clusterConfigurationService = clusterConfigurationService;
        this.clusterConnection = clusterConnection;
        this.pluginExecutorService = pluginExecutorService;
    }

    public ListenableFuture<Set<String>> getLocalConnectedClients() {
        return Futures.immediateFuture(this.channelPersistence.getConnectedClients());
    }

    public ListenableFuture<Set<String>> getLocalDisconnectedClients() {
        return this.clientSessionClusterPersistence.getLocalDisconnectedClients();
    }

    public ListenableFuture<Boolean> isClientConnectedLocal(String clientId) {
        return Futures.immediateFuture(this.channelPersistence.isClientConnected(clientId));
    }

    public ListenableFuture<ClientData> getLocalClientData(String clientId) {
        Channel channel = this.channelPersistence.getChannel(clientId);
        if (channel == null) {
            return Futures.immediateFuture(null);
        }
        return Futures.immediateFuture(ChannelUtils.clientToken(channel));
    }

    public ListenableFuture<Set<String>> getConnectedClients() {
        ListenableFuture<Set<String>> localFuture = getLocalConnectedClients();
        if (!this.clusterConfigurationService.isEnabled()) {
            return localFuture;
        }
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        Set<String> connectedClients = new HashSet<>();
        SettableFuture<Void> localSettableFuture = SettableFuture.create();
        Futures.addCallback(localFuture, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                connectedClients.addAll(result);
                localSettableFuture.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);

        ConnectedClientsRequest request = new ConnectedClientsRequest();
        ListenableFuture<Void> clusterFuture = this.clusterConnection.send(
                request, Set.class, new ClusterCallbackFactoryImpl(Set.class), true,
                (node, result) -> connectedClients.addAll(result));
        ListenableFuture<Void> future = ClusterFutures.merge(localSettableFuture, clusterFuture);
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                settableFuture.set(connectedClients);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Set<String>> getDisconnectedClients() {
        ListenableFuture<Set<String>> localFuture = getLocalDisconnectedClients();
        if (!this.clusterConfigurationService.isEnabled()) {
            return localFuture;
        }
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        Set<String> disconnectedClients = new HashSet<>();
        SettableFuture<Void> localSettableFuture = SettableFuture.create();
        Futures.addCallback(localFuture, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                disconnectedClients.addAll(result);
                localSettableFuture.set(null);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);

        DisconnectedClientsRequest request = new DisconnectedClientsRequest();
        ListenableFuture<Void> clusterFuture = this.clusterConnection.send(
                request, Set.class, new ClusterCallbackFactoryImpl(Set.class), true,
                (node, result) -> disconnectedClients.addAll(result));
        ListenableFuture<Void> future = ClusterFutures.merge(localSettableFuture, clusterFuture);
        Futures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                settableFuture.set(disconnectedClients);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Boolean> isClientConnected(String clientId) {
        if (!this.clusterConfigurationService.isEnabled()) {
            return isClientConnectedLocal(clientId);
        }
        SettableFuture<Boolean> settableFuture = SettableFuture.create();
        ListenableFuture<ClientSession> future = this.clientSessionClusterPersistence.requestSession(clientId);
        Futures.addCallback(future, new FutureCallback<ClientSession>() {

            @Override
            public void onSuccess(@Nullable ClientSession result) {
                if (result != null) {
                    settableFuture.set(result.isConnected());
                } else {
                    settableFuture.set(false);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<ClientData> getClientData(String clientId) {
        ListenableFuture<ClientData> localFuture = getLocalClientData(clientId);
        if (!this.clusterConfigurationService.isEnabled()) {
            return localFuture;
        }
        SettableFuture<ClientData> settableFuture = SettableFuture.create();
        Futures.addCallback(localFuture, new FutureCallback<ClientData>() {
            @Override
            public void onSuccess(@Nullable ClientData result) {
                if (result != null) {
                    settableFuture.set(result);
                    return;
                }
                ClientDataRequest request = new ClientDataRequest(clientId);
                ListenableFuture<Void> clusterFuture = clusterConnection.send(
                        request, ClientData.class, new ClusterCallbackFactoryImpl(ClientData.class), true,
                        (node, resultData) -> {
                            if (!settableFuture.isDone() && resultData != null) {
                                settableFuture.set(resultData);
                            }
                        });
                Futures.addCallback(clusterFuture, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable Void result) {
                        if (!settableFuture.isDone()) {
                            settableFuture.set(null);
                        }
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        settableFuture.setException(t);
                    }
                }, pluginExecutorService);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }
}
