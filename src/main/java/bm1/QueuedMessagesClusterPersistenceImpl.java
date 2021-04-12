package bm1;

import aj.ClusterFutures;
import ak.VectorClock;
import bc1.ClientSessionQueueEntry;
import bc1.QueuedMessagesLocalPersistence;
import bu.InternalPublish;
import com.codahale.metrics.MetricRegistry;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import i.PublishDispatcher;
import j1.ClusterRequestFuture;
import n1.MessageQueueDrainedAckRequestCallback;
import n1.MessageQueueOfferReplicateRequestCallback;
import n1.MessageQueuePollRequestCallback;
import n1.MessageQueueRemoveAllReplicateRequestCallback;
import n1.MessageQueueRemoveReplicateRequestCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Primary;
import t.ClusterConnection;
import u.AbstractClusterPersistence;
import u.Filter;
import u.PersistenceExecutor;
import u.TimestampObject;
import u.VectorClocks;
import v.ClientSession;
import v.ClientSessionClusterPersistence;
import v1.MessageQueueDrainedAckRequest;
import v1.MessageQueueOfferReplicateRequest;
import v1.MessageQueuePollRequest;
import v1.MessageQueueRemoveAllReplicateRequest;
import v1.MessageQueueRemoveReplicateRequest;
import v1.MessageQueueReplicateRequest;
import v1.MessageQueueReplication;
import w.QueuedMessagesClusterPersistence;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

// TODO:
@CacheScoped
public class QueuedMessagesClusterPersistenceImpl extends
        AbstractClusterPersistence<ClientSessionQueueEntry>
        implements QueuedMessagesSinglePersistence, QueuedMessagesClusterPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(QueuedMessagesClusterPersistenceImpl.class);
    private final ConsistentHashingRing primaryRing;
    private final VectorClocks vectorClocks;
    private final PersistenceExecutor persistenceExecutor;
    private final QueuedMessagesLocalPersistence queuedMessagesLocalPersistence;
    private final ClusterIdProducer clusterIdProducer;
    private final int replicateCount;
    private final Set<String> l = new HashSet<>();
    private final Map<String, a> m = new HashMap<>();
    private final Provider<ClientSessionClusterPersistence> clientSessionClusterPersistenceProvider;
    private final Provider<PublishDispatcher> publishDispatcherProvider;
    private final MetricRegistry metricRegistry;

    @Inject
    public QueuedMessagesClusterPersistenceImpl(
            @Primary ConsistentHashingRing primaryRing,
            ClusterConnection clusterConnection,
            VectorClocks vectorClocks,
            QueuedMessagesLocalPersistence queuedMessagesLocalPersistence,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService,
            Provider<ClientSessionClusterPersistence> clientSessionClusterPersistenceProvider,
            Provider<PublishDispatcher> publishDispatcherProvider,
            MetricRegistry metricRegistry) {
        super(primaryRing, clusterConnection, ClientSessionQueueEntry.class);
        this.primaryRing = primaryRing;
        this.vectorClocks = vectorClocks;
        this.clientSessionClusterPersistenceProvider = clientSessionClusterPersistenceProvider;
        this.publishDispatcherProvider = publishDispatcherProvider;
        this.metricRegistry = metricRegistry;
        this.persistenceExecutor = new PersistenceExecutor("message-queue-writer", metricRegistry);
        this.queuedMessagesLocalPersistence = queuedMessagesLocalPersistence;
        this.clusterIdProducer = clusterIdProducer;
        this.replicateCount = clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount();
    }

    public ListenableFuture<Void> offer(@NotNull String clientId, @NotNull InternalPublish publish) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(publish, "Publish must not be null");
            long timestamp = System.currentTimeMillis();
            ListenableFuture<VectorClock> future =
                    this.persistenceExecutor.add(() -> {
                        queuedMessagesLocalPersistence.offer(clientId, publish, timestamp);
                        return vectorClocks.getAndIncrement(clientId, clusterIdProducer.get());
                    });
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(future, new FutureCallback<VectorClock>() {

                @Override
                public void onSuccess(@Nullable VectorClock result) {
                    ListenableFuture<Void> f = replicateOffer(clientId, publish, timestamp, result);
                    ClusterFutures.setFuture(f, settableFuture);
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public ListenableFuture<Void> replicateOffer(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(publish, "Publish must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            MessageQueueOfferReplicateRequest request = new MessageQueueOfferReplicateRequest(clientId, requestTimestamp, requestVectorClock, publish);
            ImmutableList<ClusterRequestFuture<Void, MessageQueueOfferReplicateRequest>> futures = replicate(request);
            List<ListenableFuture<Void>> entryFutures = futures.stream()
                    .map(requestFuture -> requestFuture.setCallback(new MessageQueueOfferReplicateRequestCallback(this)))
                    .collect(Collectors.toList());
            return ClusterFutures.merge(entryFutures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    @Override
    public ListenableFuture<Void> processOfferReplica(@NotNull String clientId, @NotNull InternalPublish publish, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(publish, "Publish must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            VectorClock vc = this.vectorClocks.get(clientId);
            if (requestVectorClock.before(vc) || requestVectorClock.equals(vc)) {
                return Futures.immediateFuture(null);
            }
            if (vc.before(requestVectorClock)) {
                return this.persistenceExecutor.add(() -> {
                    vectorClocks.put(clientId, requestVectorClock);
                    queuedMessagesLocalPersistence.offer(clientId, publish, requestTimestamp);
                    return null;
                });
            }
            LOGGER.trace("Conflict for queued message for client {}. Local vector clock={}, request vector clock={}.",
                    clientId, vc, requestVectorClock);
            return this.persistenceExecutor.add(() -> {
                vc.merge(requestVectorClock);
                vc.increment(clusterConnection.getClusterId());
                vectorClocks.put(clientId, vc);
                queuedMessagesLocalPersistence.offer(clientId, publish, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<InternalPublish> processPollRequest(@NotNull String clientId,
                                                                @NotNull String requestId,
                                                                @NotNull String sender) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestId, "Request id must not be null");
            Preconditions.checkNotNull(sender, "Sender must not be null");
            return this.persistenceExecutor.add(() -> {
                a locala = new a(sender, requestId);
                if (m.containsKey(clientId) &&
                        m.get(clientId).a().equals(sender) &&
                        !m.get(clientId).equals(locala)) {
                    persistenceExecutor.add(() -> {
                        long timestamp = System.currentTimeMillis();
                        ClientSessionQueueEntry entry = queuedMessagesLocalPersistence.poll(clientId, timestamp);
                        VectorClock vectorClock = vectorClocks.getAndIncrement(clientId, clusterIdProducer.get());
                        replicateRemove(clientId, entry.getTimestamp(), entry.getUniqueId(), vectorClock, timestamp);
                        return null;
                    });
                }

                ClientSessionQueueEntry entry = queuedMessagesLocalPersistence.peek(clientId);
                m.put(clientId, locala);
                if (entry == null) {
                    l.add(clientId);
                }
                return createPublish(entry);
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> replicateRemove(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(entryId, "Entry id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            MessageQueueRemoveReplicateRequest request = new MessageQueueRemoveReplicateRequest(clientId, entryTimestamp, entryId, requestTimestamp, requestVectorClock);
            ImmutableList<ClusterRequestFuture<Void, MessageQueueRemoveReplicateRequest>> requestFutures = replicate(request);
            List<ListenableFuture<Void>> futures = requestFutures.stream()
                    .map(requestFuture -> requestFuture.setCallback(new MessageQueueRemoveReplicateRequestCallback(this)))
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public boolean b(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        return this.l.contains(clientId);
    }

    public ListenableFuture<Void> processRemoveReplica(@NotNull String clientId, long entryTimestamp, @NotNull String entryId, @NotNull VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(entryId, "Entry id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            VectorClock localVectorClock = this.vectorClocks.get(clientId);
            if (requestVectorClock.before(localVectorClock) || requestVectorClock.equals(localVectorClock)) {
                return Futures.immediateFuture(null);
            }
            if (localVectorClock.before(requestVectorClock)) {
                return this.persistenceExecutor.add(() -> {
                    vectorClocks.put(clientId, requestVectorClock);
                    queuedMessagesLocalPersistence.remove(clientId, entryId, entryTimestamp, requestTimestamp);
                    return null;
                });
            }
            LOGGER.trace("Conflict for queued message for client {}. Local vector clock={}, request vector clock={}.",
                    clientId, localVectorClock, requestVectorClock);
            return this.persistenceExecutor.add(() -> {
                localVectorClock.merge(requestVectorClock);
                localVectorClock.increment(clusterConnection.getClusterId());
                vectorClocks.put(clientId, localVectorClock);
                queuedMessagesLocalPersistence.remove(clientId, entryId, entryTimestamp, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> processRemoveAllReplica(@NotNull String clientId, long requestTimestamp, @NotNull VectorClock requestVectorClock) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            VectorClock localVectorClock = this.vectorClocks.get(clientId);
            if (requestVectorClock.before(localVectorClock) || requestVectorClock.equals(localVectorClock)) {
                return Futures.immediateFuture(null);
            }
            if (localVectorClock.before(requestVectorClock)) {
                return this.persistenceExecutor.add(() -> {
                    vectorClocks.put(clientId, requestVectorClock);
                    queuedMessagesLocalPersistence.remove(clientId, requestTimestamp);
                    return null;
                });
            }
            LOGGER.trace("Conflict for queued message for client {}. Local vector clock={}, request vector clock={}.",
                    clientId, localVectorClock, requestVectorClock);
            return this.persistenceExecutor.add(() -> {
                localVectorClock.merge(requestVectorClock);
                localVectorClock.increment(clusterConnection.getClusterId());
                vectorClocks.put(clientId, localVectorClock);
                queuedMessagesLocalPersistence.remove(clientId, requestTimestamp);
                return null;
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<InternalPublish> poll(@NotNull String clientId) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            String str = this.primaryRing.getNode(clientId);
            long l1 = System.currentTimeMillis();
            if (str.equals(this.clusterConnection.getClusterId())) {
                return this.persistenceExecutor.add(() -> {
                    ClientSessionQueueEntry entry = queuedMessagesLocalPersistence.poll(clientId, l1);
                    if (entry == null) {
                        return null;
                    }
                    InternalPublish publish = createPublish(entry);
                    VectorClock localVectorClock = vectorClocks.getAndIncrement(clientId, clusterIdProducer.get());
                    replicateRemove(clientId, entry.getTimestamp(), entry.getUniqueId(), localVectorClock, l1);
                    return publish;
                });
            }
            MessageQueuePollRequest request = new MessageQueuePollRequest(clientId);
            ClusterRequestFuture<InternalPublish, MessageQueuePollRequest> requestFuture
                    = this.clusterConnection.send(request, str, InternalPublish.class);
            return requestFuture.setCallback(new MessageQueuePollRequestCallback(this, this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }


    private InternalPublish createPublish(ClientSessionQueueEntry entry) {
        if (entry == null) {
            return null;
        }
        InternalPublish publish = new InternalPublish(entry.getSequence(), entry.getTimestamp(), entry.getClusterId());
        publish.setTopic(entry.getTopic());
        publish.setQoS(entry.getQoS());
        publish.setPayload(entry.getPayload());
        return publish;
    }

    public ListenableFuture<Void> replicateRemoveAll(String clientId, VectorClock requestVectorClock, long requestTimestamp) {
        try {
            Preconditions.checkNotNull(clientId, "Client id must not be null");
            Preconditions.checkNotNull(requestVectorClock, "Vector clock must not be null");
            MessageQueueRemoveAllReplicateRequest locale = new MessageQueueRemoveAllReplicateRequest(clientId, requestTimestamp, requestVectorClock);
            ImmutableList localImmutableList = replicate(locale);
            ArrayList localArrayList = new ArrayList();
            Iterator localIterator = localImmutableList.iterator();
            while (localIterator.hasNext()) {
                ClusterRequestFuture locale1 = (ClusterRequestFuture) localIterator.next();
                ListenableFuture localListenableFuture = locale1.setCallback(new MessageQueueRemoveAllReplicateRequestCallback(this));
                localArrayList.add(localListenableFuture);
            }
            return ClusterFutures.merge(localArrayList);
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    @Override
    public boolean isEmpty(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        return this.queuedMessagesLocalPersistence.size(clientId) == 0L ||
                b(clientId);
    }

    public ListenableFuture<Boolean> queuePublishIfQueueNotEmpty(@NotNull String clientId, @NotNull InternalPublish publish) {
        throw new UnsupportedOperationException("The method 'queuePublishIfQueueNotEmpty' is for single mode only.");
    }

    public ListenableFuture<Void> remove(@NotNull String clientId) {
        try {
            long timestamp = System.currentTimeMillis();
            ListenableFuture<VectorClock> future = this.persistenceExecutor.add(() -> {
                queuedMessagesLocalPersistence.remove(clientId, timestamp);
                return vectorClocks.getAndIncrement(clientId, clusterIdProducer.get());
            });
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(future, new FutureCallback<VectorClock>() {

                @Override
                public void onSuccess(@Nullable VectorClock result) {
                    replicateRemoveAll(clientId, result, timestamp);
                    settableFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable localThrowable) {
            return Futures.immediateFailedFuture(localThrowable);
        }
    }

    public void processAckDrained(@NotNull String clientId, @NotNull String sender) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        if (this.m.get(clientId).a().equals(sender)) {
            this.l.remove(clientId);
        }
    }

    public ListenableFuture<MessageQueueReplicateRequest> getDataForReplica(@NotNull Filter filter) {
        try {
            return this.persistenceExecutor.add(() -> {
                ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> entries = queuedMessagesLocalPersistence.getEntries(filter);
                ImmutableMap.Builder<String, MessageQueueReplication> builder = ImmutableMap.builder();
                entries.forEach((clientId, entry) -> {
                    VectorClock vectorClock = vectorClocks.get(clientId);
                    long timestamp = entry.getTimestamp();
                    MessageQueueReplication replication = new MessageQueueReplication(vectorClock, timestamp, ImmutableSet.copyOf(entry.getObject()));
                    builder.put(clientId, replication);
                });
                return new MessageQueueReplicateRequest(builder.build());
            });
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> ackDrained(String clientId) {
        try {
            MessageQueueDrainedAckRequest request = new MessageQueueDrainedAckRequest(clientId);
            String node = this.primaryRing.getNode(clientId);
            ClusterRequestFuture<Void, MessageQueueDrainedAckRequest> future = this.clusterConnection.send(request, node, Void.class);
            return future.setCallback(new MessageQueueDrainedAckRequestCallback(this));
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public ListenableFuture<Void> processReplicationRequest(@NotNull MessageQueueReplicateRequest request) {
        try {
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ListenableFuture<Void> future =
                    this.persistenceExecutor.add(() -> {
                        ImmutableMap<String, MessageQueueReplication> replications = request.getReplications();
                        List<ListenableFuture<Void>> replicationFutures = new ArrayList<>();
                        replications.forEach((clientId, replication) -> {
                            SettableFuture<Void> rf = SettableFuture.create();
                            replicationFutures.add(rf);
                            VectorClock requestVectorClock = replication.getVectorClock();
                            VectorClock localVectorClock = vectorClocks.get(clientId);
                            if ((requestVectorClock.before(localVectorClock)) || (requestVectorClock.equals(localVectorClock))) {
                                rf.set(null);
                            } else if (localVectorClock.before(requestVectorClock)) {
                                vectorClocks.put(clientId, requestVectorClock);
                                queuedMessagesLocalPersistence.offerAll(replication.getEntries(), clientId, replication.getTimestamp());
                                rf.set(null);
                            } else {
                                LOGGER.trace("Conflict for queued message for client {}. Local vector clock={}, request vector clock={}.",
                                        clientId, localVectorClock, requestVectorClock);
                                ListenableFuture<ClientSession> f = clientSessionClusterPersistenceProvider.get().getLocally(clientId);
                                ClusterFutures.addCallback(f, new FutureCallback<ClientSession>() {

                                    @Override
                                    public void onSuccess(@Nullable ClientSession result) {
                                        if (result != null && result.isConnected()) {
                                            if (primaryRing.getNode(clientId).equals(clusterConnection.getClusterId())) {
                                                ImmutableSet<ClientSessionQueueEntry> entries = replication.getEntries();
                                                entries.stream()
                                                        .map(QueuedMessagesClusterPersistenceImpl.this::createPublish)
                                                        .forEach(publish ->
                                                                publishDispatcherProvider.get().a(publish, clientId, publish.getQoS().getQosNumber(), false, false, false)
                                                        );
                                            }
                                        } else {
                                            localVectorClock.merge(requestVectorClock);
                                            localVectorClock.increment(clusterConnection.getClusterId());
                                            vectorClocks.put(clientId, localVectorClock);
                                            ImmutableSet<ClientSessionQueueEntry> entries = replication.getEntries();
                                            entries.stream()
                                                    .map(QueuedMessagesClusterPersistenceImpl.this::createPublish)
                                                    .forEach(publish ->
                                                            queuedMessagesLocalPersistence.offer(clientId, publish, System.currentTimeMillis())
                                                    );
                                        }
                                        rf.set(null);
                                    }

                                    @Override
                                    public void onFailure(Throwable t) {
                                        settableFuture.setException(t);
                                    }
                                });
                            }
                        });
                        settableFuture.setFuture(ClusterFutures.merge(replicationFutures));
                        return null;
                    });
            ClusterFutures.waitFuture(future);
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeLocally(@NotNull Filter filter) {
        this.persistenceExecutor.add(() -> {
            ImmutableSet<String> clients = queuedMessagesLocalPersistence.remove(filter);
            clients.forEach(vectorClocks::remove);
            return null;
        });
    }

    public ListenableFuture<Void> cleanUp(long tombstoneMaxAge) {
        return this.persistenceExecutor.add(() -> {
            ImmutableSet<String> queuedMessages = queuedMessagesLocalPersistence.cleanUp(tombstoneMaxAge);
            queuedMessages.forEach(vectorClocks::remove);
            return null;
        });
    }

    public void resetQueueDrained(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        this.l.remove(clientId);
    }

    protected String name() {
        return "client session queued messages";
    }

    protected int getReplicateCount() {
        return this.replicateCount;
    }

    @Override
    protected ClientSessionQueueEntry get(String key) {
        return this.queuedMessagesLocalPersistence.peek(key);
    }

    private class a {
        private final String b;
        private final String c;

        private a(String paramString1, String paramString2) {
            this.b = paramString1;
            this.c = paramString2;
        }

        public String a() {
            return this.b;
        }

        public boolean equals(Object paramObject) {
            if (this == paramObject) {
                return true;
            }
            if ((paramObject == null) || (getClass() != paramObject.getClass())) {
                return false;
            }
            a locala = (a) paramObject;
            if (this.b != null ? !this.b.equals(locala.b) : locala.b != null) {
                return false;
            }
            return this.c != null ? this.c.equals(locala.c) : locala.c == null;
        }

        public int hashCode() {
            int i = this.b != null ? this.b.hashCode() : 0;
            i = 31 * i + (this.c != null ? this.c.hashCode() : 0);
            return i;
        }
    }
}
