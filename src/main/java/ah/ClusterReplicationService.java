package ah;

import aa.TopicTreeReplicateSegmentRequest;
import aj.ClusterFutures;
import bl1.ChannelPersistence;
import bn1.OutgoingMessageFlowClusterPersistence;
import by.Segment;
import by.TopicTreeClusterPersistence;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterReplicateRequest;
import j1.ClusterRequestFuture;
import l1.ClientSessionPutAllRequestCallback;
import m1.OutgoingMessageFlowReplicateRequestCallback;
import n1.MessageQueueReplicateRequestCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import p1.RetainedMessagePutAllRequestCallback;
import q.ConsistentHashingRing;
import r1.ClientSessionSubscriptionAllReplicateRequestCallback;
import s.Primary;
import s1.TopicTreeReplicateSegmentRequestCallback;
import t.ClusterConnection;
import t1.ClientSessionPutAllRequest;
import t1.ClientSessionReplicateRequest;
import u.Filter;
import u.RemoveOutgoingMessagesReplicationFilter;
import u.RemoveReplicationFilter;
import u1.OutgoingMessageFlowReplicateRequest;
import v.ClientSessionClusterPersistence;
import v1.MessageQueueReplicateRequest;
import w.QueuedMessagesClusterPersistence;
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessagePutAllRequest;
import y.ClientSessionSubscriptionsClusterPersistence;
import z1.ClientSessionSubscriptionAllReplicateRequest;
import z1.ClientSessionSubscriptionReplicateRequest;

import javax.annotation.Nullable;

@CacheScoped
public class ClusterReplicationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterReplicationService.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;
    private final TopicTreeClusterPersistence topicTreeClusterPersistence;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;
    private final OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence;
    private final ConsistentHashingRing primaryRing;
    private final ClusterConnection clusterConnection;
    private final ChannelPersistence channelPersistence;
    private final ClusterConfigurationService clusterConfigurationService;

    @Inject
    ClusterReplicationService(
            RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
            ClientSessionClusterPersistence clientSessionClusterPersistence,
            ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
            TopicTreeClusterPersistence topicTreeClusterPersistence,
            QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
            OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence,
            @Primary ConsistentHashingRing primaryRing,
            ClusterConnection clusterConnection,
            ChannelPersistence channelPersistence,
            ClusterConfigurationService clusterConfigurationService) {
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
        this.outgoingMessageFlowClusterPersistence = outgoingMessageFlowClusterPersistence;
        this.primaryRing = primaryRing;
        this.clusterConnection = clusterConnection;
        this.channelPersistence = channelPersistence;
        this.clusterConfigurationService = clusterConfigurationService;
    }

    public ListenableFuture<Void> replicateRetainedMessages(String node, Filter filter) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<ImmutableSet<ClusterReplicateRequest>> future = this.retainedMessagesClusterPersistence.getDataForReplica(filter);
            ClusterFutures.addCallback(future, new RetainedMessagesCallback(node, settableFuture, this.clusterConnection, this, filter));
            return settableFuture;
        } catch (Throwable t) {
            return Futures.immediateFailedFuture(t);
        }
    }


    public void removeRetainedMessagesReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getRetainedMessage().getReplicateCount();
        RemoveReplicationFilter filter = new RemoveReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount);
        this.retainedMessagesClusterPersistence.removeLocally(filter);
    }


    public ListenableFuture<Void> replicateTopicTree(@NotNull String node, @NotNull Filter filter) {
        try {
            ImmutableSet<Segment> segments = this.topicTreeClusterPersistence.getLocalDate(filter);
            TopicTreeReplicateSegmentRequest request = new TopicTreeReplicateSegmentRequest(segments);
            ClusterRequestFuture requestFuture = this.clusterConnection.send(request, node, Void.class);
            return requestFuture.setCallback(new TopicTreeReplicateSegmentRequestCallback(this, filter));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }


    public void removeTopicTreeReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getTopicTree().getReplicateCount();
        RemoveReplicationFilter filter = new RemoveReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount);
        this.topicTreeClusterPersistence.removeLocally(filter);
    }

    public ListenableFuture<Void> replicateClientSessionSubscriptions(String node, Filter filter) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<ImmutableSet<ClientSessionSubscriptionReplicateRequest>> future = this.clientSessionSubscriptionsClusterPersistence.getDataForReplica(filter);
            ClusterFutures.addCallback(future,
                    new ClientSessionSubscriptionsCallback(node, settableFuture, this.clusterConnection, this, filter));
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeClientSessionSubscriptionsReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getSubscriptions().getReplicateCount();
        RemoveReplicationFilter filter = new RemoveReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount);
        this.clientSessionSubscriptionsClusterPersistence.removeLocally(filter);
    }

    public ListenableFuture<Void> replicateClientSession(String node, Filter filter) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<ImmutableSet<ClientSessionReplicateRequest>> future =
                    this.clientSessionClusterPersistence.getDataForReplica(filter);
            ClusterFutures.addCallback(future,
                    new ClientSessionCallback(node, filter, settableFuture, this.clusterConnection, this));
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeClientSessionReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getClientSession().getReplicateCount();
        RemoveReplicationFilter filter = new RemoveReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount);
        this.clientSessionClusterPersistence.removeLocally(filter);
    }

    public ListenableFuture<Void> replicateQueuedMessages(String node, Filter filter) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<MessageQueueReplicateRequest> future = this.queuedMessagesClusterPersistence.getDataForReplica(filter);
            ClusterFutures.addCallback(future, new FutureCallback<MessageQueueReplicateRequest>() {

                @Override
                public void onSuccess(@Nullable MessageQueueReplicateRequest result) {
                    ClusterRequestFuture<Void, MessageQueueReplicateRequest> requestFuture = clusterConnection.send(result, node, Void.class);
                    settableFuture.setFuture(requestFuture.setCallback(new MessageQueueReplicateRequestCallback(ClusterReplicationService.this, filter)));
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeQueuedMessagesReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount();
        RemoveReplicationFilter filter = new RemoveReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount);
        this.queuedMessagesClusterPersistence.removeLocally(filter);
    }


    public ListenableFuture<Void> replicateOutgoingMessageFlow(String node, Filter filter) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<OutgoingMessageFlowReplicateRequest> future = this.outgoingMessageFlowClusterPersistence.getLocalData(filter);
            ClusterFutures.addCallback(future, new FutureCallback<OutgoingMessageFlowReplicateRequest>() {

                @Override
                public void onSuccess(@Nullable OutgoingMessageFlowReplicateRequest result) {
                    ClusterRequestFuture<Void, OutgoingMessageFlowReplicateRequest> requestFuture = clusterConnection.send(result, node, Void.class);
                    settableFuture.setFuture(requestFuture.setCallback(new OutgoingMessageFlowReplicateRequestCallback(ClusterReplicationService.this, filter)));
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeOutgoingMessageFlowReplication() {
        int replicateCount = this.clusterConfigurationService.getReplicates().getOutgoingMessageFlow().getReplicateCount();
        RemoveOutgoingMessagesReplicationFilter filter = new RemoveOutgoingMessagesReplicationFilter(this.clusterConnection, this.primaryRing, replicateCount, this.channelPersistence);
        this.outgoingMessageFlowClusterPersistence.removeLocally(filter);
    }


    private static class ClientSessionCallback
            implements FutureCallback<ImmutableSet<ClientSessionReplicateRequest>> {
        private final String node;
        private final Filter filter;
        private final SettableFuture<Void> settableFuture;
        private final ClusterConnection clusterConnection;
        private final ClusterReplicationService clusterReplicationService;

        public ClientSessionCallback(
                String node,
                Filter filter,
                SettableFuture<Void> settableFuture,
                ClusterConnection clusterConnection,
                ClusterReplicationService clusterReplicationService) {
            this.node = node;
            this.filter = filter;
            this.settableFuture = settableFuture;
            this.clusterConnection = clusterConnection;
            this.clusterReplicationService = clusterReplicationService;
        }

        @Override
        public void onSuccess(@Nullable ImmutableSet<ClientSessionReplicateRequest> result) {
            ClientSessionPutAllRequest request = new ClientSessionPutAllRequest(result);
            ClusterRequestFuture requestFuture = this.clusterConnection.send(request, this.node, Void.class);
            this.settableFuture.setFuture(requestFuture.setCallback(new ClientSessionPutAllRequestCallback(this.clusterReplicationService, this.filter)));
        }

        public void onFailure(Throwable t) {
            this.settableFuture.setException(t);
        }
    }

    private static class ClientSessionSubscriptionsCallback
            implements FutureCallback<ImmutableSet<ClientSessionSubscriptionReplicateRequest>> {
        private final String node;
        private final SettableFuture<Void> settableFuture;
        private final ClusterConnection clusterConnection;
        private final ClusterReplicationService clusterReplicationService;
        private final Filter filter;

        public ClientSessionSubscriptionsCallback(
                String node,
                SettableFuture<Void> settableFuture,
                ClusterConnection clusterConnection,
                ClusterReplicationService clusterReplicationService,
                Filter filter) {
            this.node = node;
            this.settableFuture = settableFuture;
            this.clusterConnection = clusterConnection;
            this.clusterReplicationService = clusterReplicationService;
            this.filter = filter;
        }

        @Override
        public void onSuccess(@Nullable ImmutableSet<ClientSessionSubscriptionReplicateRequest> result) {
            ClientSessionSubscriptionAllReplicateRequest request = new ClientSessionSubscriptionAllReplicateRequest(result);
            ClusterRequestFuture<Void, ClientSessionSubscriptionAllReplicateRequest> requestFuture
                    = this.clusterConnection.send(request, this.node, Void.class);
            ListenableFuture<Void> future = requestFuture.setCallback(
                    new ClientSessionSubscriptionAllReplicateRequestCallback(
                            this.clusterReplicationService, this.filter));
            this.settableFuture.setFuture(future);
        }

        public void onFailure(Throwable t) {
            this.settableFuture.setException(t);
        }
    }


    private static class RetainedMessagesCallback
            implements FutureCallback<ImmutableSet<ClusterReplicateRequest>> {
        private final String node;
        private final SettableFuture<Void> settableFuture;
        private final ClusterConnection clusterConnection;
        private final ClusterReplicationService clusterReplicationService;
        private final Filter filter;

        public RetainedMessagesCallback(String node,
                                        SettableFuture<Void> settableFuture,
                                        ClusterConnection clusterConnection,
                                        ClusterReplicationService clusterReplicationService,
                                        Filter filter) {
            this.node = node;
            this.settableFuture = settableFuture;
            this.clusterConnection = clusterConnection;
            this.clusterReplicationService = clusterReplicationService;
            this.filter = filter;
        }

        @Override
        public void onSuccess(@Nullable ImmutableSet<ClusterReplicateRequest> result) {
            RetainedMessagePutAllRequest request = new RetainedMessagePutAllRequest(result);
            ClusterRequestFuture<Void, RetainedMessagePutAllRequest> requestFuture = this.clusterConnection.send(request, this.node, Void.class);
            ListenableFuture<Void> future = requestFuture.setCallback(
                    new RetainedMessagePutAllRequestCallback(this.clusterReplicationService, this.filter));
            this.settableFuture.setFuture(future);
        }

        public void onFailure(Throwable t) {
            this.settableFuture.setException(t);
        }
    }
}
