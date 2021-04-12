package t;

import a1.ClusterRequestDispatcher;
import ab.ClusterResponseCode;
import ab.ClusterResult;
import ab.ClusterSettableResponse;
import ac.SerializationService;
import ah.ClusterContext;
import aj.ClusterFutures;
import av.InternalConfigurationService;
import av.Internals;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Provider;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import d.CacheScoped;
import i.ClusterIdProducer;
import j1.ClusterRequest;
import j1.ClusterRequestFuture;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.blocks.MessageDispatcher;
import org.jgroups.blocks.RequestOptions;
import org.jgroups.blocks.ResponseMode;
import org.jgroups.blocks.RspFilter;
import org.jgroups.protocols.pbcast.GMS;
import org.jgroups.stack.ProtocolStack;
import org.jgroups.util.NotifyingFuture;
import org.jgroups.util.RspList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p.ChannelNotAvailableException;
import p.ClusterTimeoutException;
import q.ConsistentHashingRing;
import s.Cluster;
import s.Minority;
import s.Primary;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CacheScoped
public class ClusterConnection {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterConnection.class);
    public static final String CLUSTER_NAME = "HMQ";
    private final ConsistentHashingRing primaryRing;
    private final ConsistentHashingRing minorityRing;
    private final ClusterProtocolStackConfigurer protocolStackConfigurer;
    private final ClusterIdProducer clusterIdProducer;
    private final ExecutorService clusterExecutor;
    @VisibleForTesting
    protected MessageDispatcher messageDispatcher;
    @VisibleForTesting
    protected ClusterReceiverService clusterReceiverService;
    @VisibleForTesting
    protected JChannel jChannel;
    private final SerializationService serializationService;
    private final String clusterId;
    private final ListeningScheduledExecutorService clusterScheduledExecutor;
    private final Provider<ClusterRequestDispatcher> clusterRequestDispatcherProvider;
    private final InternalConfigurationService internalConfigurationService;
    private final ClusterContext clusterContext;
    private final long timeout;

    @Inject
    ClusterConnection(@Primary ConsistentHashingRing primaryRing,
                      @Minority ConsistentHashingRing minorityRing,
                      ClusterProtocolStackConfigurer protocolStackConfigurer,
                      ClusterIdProducer clusterIdProducer,
                      @Cluster ExecutorService clusterExecutor,
                      SerializationService serializationService,
                      @Cluster ListeningScheduledExecutorService clusterScheduledExecutor,
                      Provider<ClusterRequestDispatcher> clusterRequestDispatcherProvider,
                      InternalConfigurationService internalConfigurationService,
                      ClusterContext clusterContext) throws Exception {
        this.primaryRing = primaryRing;
        this.minorityRing = minorityRing;
        this.protocolStackConfigurer = protocolStackConfigurer;
        this.clusterIdProducer = clusterIdProducer;
        this.clusterExecutor = clusterExecutor;
        this.serializationService = serializationService;
        this.clusterScheduledExecutor = clusterScheduledExecutor;
        this.clusterRequestDispatcherProvider = clusterRequestDispatcherProvider;
        this.internalConfigurationService = internalConfigurationService;
        this.clusterContext = clusterContext;
        this.jChannel = new JChannel();
        this.clusterId = this.clusterIdProducer.get();
        this.timeout = internalConfigurationService.getLong(Internals.CLUSTER_REQUEST_DEFAULT_TIMEOUT);
    }

    public void connect(@NotNull ClusterReceiverService clusterReceiverService,
                        @NotNull MessageDispatcher messageDispatcher) throws Exception {
        Preconditions.checkNotNull(clusterReceiverService, "Cluster receiver must not be null");
        Preconditions.checkNotNull(messageDispatcher, "Message dispatcher must not be null");
        this.clusterReceiverService = clusterReceiverService;
        this.messageDispatcher = messageDispatcher;
        this.jChannel.setName(this.clusterId);
        ProtocolStack protocolStack = this.protocolStackConfigurer.getProtocolStack();
        this.jChannel.setProtocolStack(protocolStack);
        protocolStack.init();
        GMS gms = (GMS) this.jChannel.getProtocolStack().findProtocol(GMS.class);
        gms.setMembershipChangePolicy(new ClusterMembershipChangePolicy());
        this.jChannel.setReceiver(this.clusterReceiverService);
        this.jChannel.connect(CLUSTER_NAME, null,
                this.internalConfigurationService.getInt(Internals.CLUSTER_CONNECT_TIMEOUT));
    }

    public void disconnect() {
        try {
            if (this.jChannel.isConnected()) {
                this.jChannel.disconnect();
            }
            if (!this.jChannel.isClosed()) {
                this.jChannel.close();
            }
        } catch (IllegalStateException e) {
        }
    }

    public <S, Q extends ClusterRequest> ListenableFuture<Void> send(Q request,
                                                                     Class<S> returnType,
                                                                     ClusterCallbackFactory<S, Q> callbackFactory,
                                                                     boolean excludeLocal) {
        return send(request, returnType, callbackFactory, excludeLocal, null);
    }

    public <S, Q extends ClusterRequest> ListenableFuture<Void> send(@NotNull Q request,
                                                                     @NotNull Class<S> returnType,
                                                                     @NotNull ClusterCallbackFactory<S, Q> callbackFactory,
                                                                     boolean excludeLocal,
                                                                     @Nullable ClusterResponseHandler<S> responseHandler) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(returnType, "Return type must not be null");
        Preconditions.checkNotNull(callbackFactory, "Callback factory must not be null");
        try {
            Set<String> nodes = Sets.newHashSet(this.primaryRing.getNodes());
            if (excludeLocal) {
                nodes.remove(this.clusterId);
            }
            List<ListenableFuture<Void>> futures = nodes.stream()
                    .map(node -> {
                        SettableFuture<Void> settableFuture = SettableFuture.create();
                        ClusterRequestFuture requestFuture = send(request, node, returnType);
                        ListenableFuture<S> future = requestFuture.setCallback(callbackFactory.create());
                        ClusterFutures.addCallback(future, new FutureCallback<S>() {

                            @Override
                            public void onSuccess(@javax.annotation.Nullable S result) {
                                if (responseHandler != null) {
                                    responseHandler.handle(node, result);
                                }
                                settableFuture.set(null);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                settableFuture.setException(t);
                            }
                        });

                        return settableFuture;
                    })
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Exception e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(Q request,
                                                                         String node,
                                                                         Class<S> returnType) {
        Address address = this.clusterContext.getAddress(node);
        return send(request, address, returnType, this.timeout, this.clusterExecutor);
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(
            Q request,
            Address dest,
            Class<S> paramClass) {
        return send(request, dest, paramClass, this.timeout, this.clusterExecutor);
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(
            Q request,
            String node,
            Class<S> returnType,
            long timeout) {
        Address dest = this.clusterContext.getAddress(node);
        return send(request, dest, returnType, timeout, this.clusterExecutor);
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(Q request,
                                                                         Address dest,
                                                                         Class<S> resultType,
                                                                         long timeout) {
        return send(request, dest, resultType, timeout, this.clusterExecutor);
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(Q request,
                                                                         String node,
                                                                         Class<S> resultType,
                                                                         long timeout,
                                                                         ExecutorService executorService) {
        Address dest = this.clusterContext.getAddress(node);
        return send(request, dest, resultType, timeout, executorService);
    }

    public <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(@NotNull Q request,
                                                                         @NotNull Address dest,
                                                                         @NotNull Class<S> resultType,
                                                                         long timeout,
                                                                         @NotNull ExecutorService executorService) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(dest, "Address must not be null");
        Preconditions.checkNotNull(resultType, "Result type must not be null");
        Preconditions.checkNotNull(executorService, "Executor service must not be null");
        if (!this.jChannel.isOpen() ||
                !this.jChannel.isConnected() ||
                this.jChannel.isClosed()) {
            return new ClusterRequestFuture(
                    Futures.immediateFailedFuture(new ChannelNotAvailableException()),
                    null, this, request, timeout, this.clusterScheduledExecutor);
        }
        if (this.jChannel.getAddress().equals(dest)) {
            return send(request, timeout);
        }
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMode(ResponseMode.GET_ALL);
        Message message = new Message(dest, this.serializationService.serialize(request));
        SettableFuture<S> settableFuture = SettableFuture.create();
        NotifyingFuture notifyingFuture;
        try {
            notifyingFuture = this.messageDispatcher.sendMessageWithFuture(message, requestOptions);
        } catch (Exception e) {
            LOGGER.debug("Exception while sending message.", e);
            settableFuture.setException(e);
            return new ClusterRequestFuture(settableFuture, this.jChannel.getName(dest), this, request, timeout, this.clusterScheduledExecutor);
        }
        ListenableScheduledFuture<?> scheduledFuture = setTimeout(notifyingFuture, settableFuture, timeout);
        ListenableFuture<?> future = setNotifyingListener(notifyingFuture);
        Futures.addCallback(future, new FutureCallback<Object>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable Object result) {
                try {
                    if (scheduledFuture.cancel(false)) {
                        handleResult(settableFuture, (byte[]) result, request);
                    }
                } catch (CancellationException e) {
                    LOGGER.debug("Interrupted retry.", e);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, executorService);
        return new ClusterRequestFuture(settableFuture, this.jChannel.getName(dest),
                this, request, timeout, this.clusterScheduledExecutor);
    }

    private <S, Q extends ClusterRequest> ClusterRequestFuture<S, Q> send(Q request, long timeout) {
        SettableFuture<S> settableFuture = SettableFuture.create();
        ClusterSettableResponse<S> response = new ClusterSettableResponse<>(this.serializationService, this, null, settableFuture);
        this.clusterRequestDispatcherProvider.get()
                .dispatch(request, response, this.clusterIdProducer.get());
        return new ClusterRequestFuture(settableFuture, this.clusterIdProducer.get(), this, request, timeout, this.clusterScheduledExecutor);
    }

    private ListenableScheduledFuture<?> setTimeout(NotifyingFuture<Object> notifyingFuture,
                                                    SettableFuture<?> settableFuture,
                                                    long delay) {
        return this.clusterScheduledExecutor.schedule(
                () -> {
                    notifyingFuture.cancel(true);
                    settableFuture.setException(new ClusterTimeoutException());
                }, delay, TimeUnit.MILLISECONDS);

    }

    public <S, Q extends ClusterRequest> List<ClusterRequestFuture<S, Q>> send(@NotNull Q request,
                                                                               boolean paramBoolean,
                                                                               @NotNull Class<S> resultType) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(resultType, "Result type must not be null");
        long timeout = this.timeout;
        DefaultRspFilter rspFilter = new DefaultRspFilter();
        RequestOptions requestOptions = new RequestOptions();
        requestOptions.setMode(ResponseMode.GET_ALL);
        requestOptions.setRspFilter(rspFilter);
        List<Address> members = Lists.newArrayList(this.jChannel.getView().getMembers());
        if (!paramBoolean) {
            members.remove(this.jChannel.getAddress());
            requestOptions.setTransientFlags(Message.TransientFlag.DONT_LOOPBACK);
        }
        Map<Address, SettableFuture<S>> memberFutures = new HashMap<>();
        members.forEach(member -> memberFutures.put(member, SettableFuture.create()));
        Message message = new Message(null, this.serializationService.serialize(request));
        NotifyingFuture<RspList<Object>> notifyingFuture;
        try {
            notifyingFuture = this.messageDispatcher.castMessageWithFuture(members, message, requestOptions);
        } catch (Exception e) {
            memberFutures.values().forEach(settableFuture -> settableFuture.setException(e));
            return createRequestFutures(memberFutures, request, timeout);
        }
        ListenableFuture<RspList<Object>> future = setNotifyingListener(notifyingFuture);
        this.clusterScheduledExecutor.schedule(() -> rspFilter.done(), timeout, TimeUnit.MILLISECONDS);
        Futures.addCallback(future, new FutureCallback<RspList<Object>>() {

            @Override
            public void onSuccess(@javax.annotation.Nullable RspList<Object> result) {
                result.forEach(rsp -> {
                    SettableFuture settableFuture = memberFutures.get(rsp.getSender());
                    if (rsp.getValue() == null) {
                        settableFuture.setException(new ClusterTimeoutException());
                    } else {
                        handleResult(settableFuture, (byte[]) rsp.getValue(), request);
                    }
                });
            }

            @Override
            public void onFailure(Throwable t) {
                memberFutures.values().forEach(
                        settableFuture -> settableFuture.setException(t));
            }
        }, this.clusterExecutor);
        return createRequestFutures(memberFutures, request, timeout);
    }


    private <S, Q extends ClusterRequest> void handleResult(SettableFuture<S> settableFuture,
                                                            byte[] result,
                                                            Q request) {
        try {
            ClusterResult<S> clusterResult = (ClusterResult<S>) this.serializationService.deserialize(result);
            ClusterResponseCode code = clusterResult.getCode();
            if (code == ClusterResponseCode.OK) {
                settableFuture.set(clusterResult.getData());
            } else if (!clusterResult.isValidLicense()) {
                settableFuture.setException(ClusterResult.createException(request, !clusterResult.isValidLicense(), code));
            } else {
                settableFuture.setException(code.createException());
            }
        } catch (Throwable e) {
            LOGGER.error("Exception while processing cluster response.", e);
            settableFuture.setException(e);
        }
    }


    private <T> ListenableFuture<T> setNotifyingListener(NotifyingFuture<T> notifyingFuture) {
        SettableFuture<T> settableFuture = SettableFuture.create();
        notifyingFuture.setListener(future -> {
            try {
                settableFuture.set(future.get());
            } catch (ExecutionException e) {
                settableFuture.setException(e.getCause());
            } catch (Exception e) {
                settableFuture.setException(e);
            }
        });
        return settableFuture;
    }


    private <S, Q extends ClusterRequest> List<ClusterRequestFuture<S, Q>> createRequestFutures(
            Map<Address, SettableFuture<S>> resultFutures,
            Q request, long timeout) {
        List<ClusterRequestFuture<S, Q>> requestFutures = new ArrayList<>();
        resultFutures.forEach((member, settableFuture) -> {
            String node = this.jChannel.getName(member);
            requestFutures.add(new ClusterRequestFuture(settableFuture, node, this,
                    request, timeout, this.clusterScheduledExecutor));
        });
        return requestFutures;
    }

    @NotNull
    public Set<String> getReplicaNodes(@NotNull String key, int replicateCount) {
        Preconditions.checkNotNull(key, "Key must not be null");
        if (!this.jChannel.isOpen() ||
                !this.jChannel.isConnected() ||
                this.jChannel.isClosed()) {
            return Sets.newHashSet();
        }
        int memberCount = this.jChannel.getView().size();
        int replicates = replicateCount < memberCount - 1 ? replicateCount : memberCount - 1;
        Set<String> nodes = new HashSet<>();
        nodes.addAll(this.primaryRing.getReplicaNodes(key, replicates));
        nodes.addAll(this.minorityRing.getReplicaNodes(key, replicates));
        nodes.add(this.minorityRing.getNode(key));
        nodes.remove(this.primaryRing.getNode(key));
        return nodes;
    }

    public String getClusterId() {
        return clusterId;
    }

    public JChannel getJChannel() {
        return jChannel;
    }

    private class DefaultRspFilter implements RspFilter {
        private boolean done;

        private DefaultRspFilter() {
        }

        public boolean isAcceptable(Object response, Address sender) {
            return true;
        }

        public boolean needMoreResponses() {
            return !this.done;
        }

        public void done() {
            this.done = true;
        }
    }
}
