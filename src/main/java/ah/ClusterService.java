package ah;

import aa1.LicenseType;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import af1.LicenseInformationService;
import aj.ClusterFutures;
import av.InternalConfigurationService;
import cb1.PluginExceptionUtils;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterRequestFuture;
import k.ClusterReplicates;
import k1.ClusterCallbackFactoryImpl;
import org.jgroups.Address;
import org.jgroups.JChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import q1.ClusterStateNotificationRequestCallback;
import q1.StateRequestCallback;
import s.Cluster;
import s.Minority;
import s.Primary;
import t.ClusterConnection;
import u.Filter;
import u.MinorityReplicationFilter;
import u.PrimaryReplicationFilter;
import v.ClientSessionClusterPersistence;
import y1.AllClusterStatesRequest;
import y1.AllClusterStatesRequestCallback;
import y1.ClusterStateNotificationRequest;
import y1.ClusterStateRequest;
import y1.LicenseCheckRequest;
import y1.LicenseState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * ================Primary================
 * RUNNING
 * ================Minority================
 * RUNNING
 * JOINING
 * MERGING
 * ================OTHER================
 * UNKNOWN
 * NOT_JOINED
 * MERGE_MINORITY
 * SHUTTING_DOWN
 * SHUTDOWN_FINISHED
 * DEAD
 **/


// TODO:
@CacheScoped
public class ClusterService {
    private static final int MIN_NODE = 1;
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterService.class);
    private final List<String> primaryReplicatingNodes = Collections.synchronizedList(new ArrayList<>());
    private final List<String> minorityReplicatingNodes = Collections.synchronizedList(new ArrayList<>());
    private final ClusterStateService clusterStateService;
    private final ClusterConnection clusterConnection;
    private final ConsistentHashingRing minorityRing;
    private final ConsistentHashingRing primaryRing;
    private final ClusterReplicationService clusterReplicationService;
    private final InternalConfigurationService internalConfigurationService;
    private final ListeningScheduledExecutorService clusterScheduledExecutorService;
    private final ListeningExecutorService clusterExecutor;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final ClusterConfigurationService clusterConfigurationService;
    private final ClusterContext clusterContext;
    private final LicenseInformationService licenseInformationService;

    @Inject
    ClusterService(ClusterStateService clusterStateService,
                   ClusterConnection clusterConnection,
                   @Minority ConsistentHashingRing minorityRing,
                   @Primary ConsistentHashingRing primaryRing,
                   ClusterReplicationService clusterReplicationService,
                   InternalConfigurationService internalConfigurationService,
                   @Cluster ListeningScheduledExecutorService clusterScheduledExecutor,
                   @Cluster ListeningExecutorService clusterExecutor,
                   ClientSessionClusterPersistence clientSessionClusterPersistence,
                   ClusterConfigurationService clusterConfigurationService,
                   ClusterContext clusterContext,
                   LicenseInformationService licenseInformationService) {
        this.clusterStateService = clusterStateService;
        this.clusterConnection = clusterConnection;
        this.minorityRing = minorityRing;
        this.primaryRing = primaryRing;
        this.clusterReplicationService = clusterReplicationService;
        this.internalConfigurationService = internalConfigurationService;
        this.clusterScheduledExecutorService = clusterScheduledExecutor;
        this.clusterExecutor = clusterExecutor;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.clusterConfigurationService = clusterConfigurationService;
        this.clusterContext = clusterContext;
        this.licenseInformationService = licenseInformationService;
    }

    public ListenableFuture<Void> sendStateToCoordinator(ClusterState state) {
        Address coordinatorAddress = this.clusterConnection.getJChannel().getView().getMembers().get(0);
        ClusterStateRequest request = new ClusterStateRequest(state);
        LOGGER.debug("Sending {} state request to {}.", state, coordinatorAddress);
        ClusterRequestFuture<Void, ClusterStateRequest> future = this.clusterConnection.send(request, coordinatorAddress, Void.class);
        return future.setCallback(new StateRequestCallback(this));
    }

    public ListenableFuture<Void> start() {
        try {
            checkLicense();
            String clusterId = this.clusterConnection.getClusterId();
            if (this.clusterConnection.getJChannel().getView().getMembers().size() <= MIN_NODE) {
                this.clusterStateService.setState(clusterId, ClusterState.RUNNING);
                this.minorityRing.add(this.clusterConnection.getClusterId());
                this.primaryRing.add(this.clusterConnection.getClusterId());
                return Futures.immediateFuture(null);
            }
            SettableFuture<Void> startFuture = SettableFuture.create();
            ListenableFuture<Void> notJoinedStateFuture = sendStateToCoordinator(ClusterState.NOT_JOINED);
            ClusterFutures.addCallback(notJoinedStateFuture, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    clusterStateService.change(clusterId, ClusterState.NOT_JOINED);
                    ListenableFuture<Void> joiningStateFuture = sendStateToCoordinator(ClusterState.JOINING);
                    ClusterFutures.addCallback(joiningStateFuture, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(@Nullable Void result) {
                            clusterStateService.change(clusterId, ClusterState.JOINING);
                            minorityRing.add(clusterId);
                            ListenableFuture<Void> replicateFinishedFuture = new ReplicateFinishedMonitor(clusterStateService, clusterConnection,
                                    internalConfigurationService, clusterScheduledExecutorService).start();
                            ClusterFutures.addCallback(replicateFinishedFuture, new FutureCallback<Void>() {

                                @Override
                                public void onSuccess(@Nullable Void result) {
                                    ListenableFuture<Void> runningStateFuture = sendStateToCoordinator(ClusterState.RUNNING);
                                    ClusterFutures.addCallback(runningStateFuture, new FutureCallback<Void>() {

                                        @Override
                                        public void onSuccess(@Nullable Void result) {
                                            clusterStateService.change(clusterConnection.getClusterId(), ClusterState.RUNNING);
                                            primaryRing.add(clusterId);
                                            startFuture.set(null);
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            startFuture.setException(t);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    startFuture.setException(t);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            startFuture.setException(t);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    startFuture.setException(t);
                }
            });
            return startFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    private void checkLicense() {
        this.licenseInformationService.register((oldLicense, newLicense) -> {
            LicenseState licenseState;
            if (newLicense.getLicenseType() == LicenseType.NONE) {
                licenseState = LicenseState.NONE;
            } else if (newLicense.getLicense().isSiteLicense()) {
                licenseState = LicenseState.CLUSTER;
            } else {
                licenseState = LicenseState.SINGLE;
            }
            String licenseId;
            if (licenseState == LicenseState.NONE) {
                licenseId = null;
            } else {
                licenseId = newLicense.getId();
            }
            AtomicBoolean validLicense = new AtomicBoolean(true);
            LicenseCheckRequest request = new LicenseCheckRequest(licenseState, licenseId);
            ListenableFuture future = clusterConnection.send(
                    request, Boolean.class, new ClusterCallbackFactoryImpl(Boolean.class), false,
                    (node, result) -> {
                        if (!result) {
                            validLicense.set(false);
                        }
                    });
            ClusterFutures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    if (!validLicense.get()) {
                        LOGGER.warn("There is already a HiveMQ instance with the same single license file in the cluster.");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.warn("Exception during cluster license check.", t);
                }
            });

        });
    }

    public void disconnected(Address address) {
        JChannel jChannel = this.clusterConnection.getJChannel();
        String node = jChannel.getName(address);
        this.minorityRing.remove(node);
        this.clusterStateService.remove(node);
        this.clusterContext.remove(node);
        if (!jChannel.isOpen() ||
                !jChannel.isConnected() ||
                jChannel.isClosed() ||
                jChannel.getView().getMembers().size() == 1) {
            this.clusterStateService.change(this.clusterConnection.getClusterId(), ClusterState.RUNNING);
        }
        this.clientSessionClusterPersistence.disconnectAllClients(node);
        if (this.primaryRing.getNodes().contains(node)) {
            this.primaryRing.remove(node);
            c();
        }
    }

    public void connected(Address address) {
        String node = this.clusterConnection.getJChannel().getName(address);
        this.clusterContext.add(node);
        if (!this.clusterStateService.containsNode(node)) {
            this.clusterStateService.change(node, ClusterState.UNKNOWN);
        }
    }

    public void onMerge(Set<Address> members) {
        members.forEach(member -> {
            String node = this.clusterConnection.getJChannel().getName(member);
            this.clusterStateService.change(node, ClusterState.MERGE_MINORITY);
            this.minorityRing.remove(node);
            this.primaryRing.remove(node);
        });
        if (members.contains(this.clusterConnection.getJChannel().getAddress())) {
            joinPrimary();
            return;
        }
        members.stream()
                .map(member -> this.clusterConnection.getJChannel().getName(member))
                .filter(Objects::nonNull)
                .forEach(node -> {
                    this.minorityRing.remove(node);
                    this.primaryRing.remove(node);
                });
    }

    private void joinPrimary() {
        ListenableFuture<Void> future = mergeToPrimary();
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {

            }

            @Override
            public void onFailure(Throwable t) {
                PluginExceptionUtils.logOrThrow("Unable to join primary partition on merge. Node is: " + clusterConnection.getClusterId() + ".", t);
                mergeToMinority();
            }
        });
    }

    private void mergeToMinority() {
        ListenableFuture<Void> mergeMinorityStateFuture = sendStateToCoordinator(ClusterState.MERGE_MINORITY);
        ClusterFutures.addCallback(mergeMinorityStateFuture, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                clusterStateService.change(clusterConnection.getClusterId(), ClusterState.MERGE_MINORITY);
                primaryRing.remove(clusterConnection.getClusterId());
                mergeToPrimary();
            }

            @Override
            public void onFailure(Throwable t) {
                PluginExceptionUtils.logOrThrow("Unable to return to minority partition after failed merge. Node is: " +
                        clusterConnection.getClusterId() + ".", t);
                mergeToMinority();
            }
        });
    }

    private ListenableFuture<Void> mergeToPrimary() {
        try {
            SettableFuture<Void> mergeFuture = SettableFuture.create();
            ListenableFuture<Map> statesFuture = getClusterStates();
            ClusterFutures.addCallback(statesFuture, new FutureCallback<Map>() {

                @Override
                public void onSuccess(@Nullable Map result) {
                    restConsistentHashingRing(result);
                    ListenableFuture<Void> mergingStateFuture = sendStateToCoordinator(ClusterState.MERGING);
                    ClusterFutures.addCallback(mergingStateFuture, new FutureCallback<Void>() {

                        @Override
                        public void onSuccess(@Nullable Void result) {
                            clusterStateService.change(clusterConnection.getClusterId(), ClusterState.MERGING);
                            minorityRing.add(clusterConnection.getClusterId());
                            ListenableFuture<Void> replicationFuture = primaryReplication();
                            ClusterFutures.addCallback(replicationFuture, new FutureCallback<Void>() {

                                @Override
                                public void onSuccess(@Nullable Void result) {
                                    ListenableFuture<Void> replicationFinishedFuture = new ReplicateFinishedMonitor(
                                            clusterStateService, clusterConnection,
                                            internalConfigurationService, clusterScheduledExecutorService).start();
                                    ClusterFutures.addCallback(replicationFinishedFuture, new FutureCallback<Void>() {

                                        @Override
                                        public void onSuccess(@Nullable Void result) {
                                            ListenableFuture<Void> runningStateFuture = sendStateToCoordinator(ClusterState.RUNNING);
                                            ClusterFutures.addCallback(runningStateFuture, new FutureCallback<Void>() {

                                                @Override
                                                public void onSuccess(@Nullable Void result) {
                                                    clusterStateService.change(clusterConnection.getClusterId(), ClusterState.RUNNING);
                                                    primaryRing.add(clusterConnection.getClusterId());
                                                    removeReplication(clusterConnection.getClusterId());
                                                    mergeFuture.set(null);
                                                }

                                                @Override
                                                public void onFailure(Throwable t) {
                                                    mergeFuture.setException(t);
                                                }
                                            });
                                        }

                                        @Override
                                        public void onFailure(Throwable t) {
                                            mergeFuture.setException(t);
                                        }
                                    });
                                }

                                @Override
                                public void onFailure(Throwable t) {
                                    mergeFuture.setException(t);
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            mergeFuture.setException(t);
                        }
                    });
                }

                @Override
                public void onFailure(Throwable t) {
                    mergeFuture.setException(t);
                }
            });
            return mergeFuture;
        } catch (Throwable t) {
            return Futures.immediateFailedFuture(t);
        }
    }

    private void restConsistentHashingRing(Map<String, ClusterState> clusterNodeStates) {
        clusterNodeStates.forEach((node, state) -> {
            switch (state) {
                case UNKNOWN:
                    this.primaryRing.remove(node);
                    this.minorityRing.remove(node);
                    break;
                case NOT_JOINED:
                    this.primaryRing.remove(node);
                    this.minorityRing.remove(node);
                    break;
                case JOINING:
                    this.primaryRing.remove(node);
                    this.minorityRing.add(node);
                    break;
                case RUNNING:
                    this.primaryRing.add(node);
                    this.minorityRing.add(node);
                    break;
                case MERGE_MINORITY:
                    this.primaryRing.remove(node);
                    this.minorityRing.remove(node);
                    break;
                case MERGING:
                    this.primaryRing.remove(node);
                    this.minorityRing.add(node);
                    break;
                case SHUTTING_DOWN:
                    throw new IllegalArgumentException("SHUTTING_DOWN state not implemented jet.");
                case SHUTDOWN_FINISHED:
                    throw new IllegalArgumentException("SHUTDOWN_FINISHED state not implemented jet.");
                case DEAD:
                    throw new IllegalArgumentException("DEAD state should never be assigned.");
            }
            this.clusterStateService.change(node, state);
        });
    }

    public ListenableFuture<Map> getClusterStates() {
        Address coordinatorAddress = this.clusterConnection.getJChannel().getView().getMembers().get(0);
        String coordinatorNodeName = this.clusterConnection.getJChannel().getName(coordinatorAddress);
        AllClusterStatesRequest request = new AllClusterStatesRequest();
        ClusterRequestFuture<Map, AllClusterStatesRequest> future = this.clusterConnection.send(request, coordinatorNodeName, Map.class);
        return future.setCallback(new AllClusterStatesRequestCallback(this));
    }

    private ListenableFuture<Void> primaryReplication() {
        try {
            Set<String> nodes = this.minorityRing.getNodes();
            List<ListenableFuture<Void>> futures = nodes.stream()
                    .filter(node -> !node.equals(this.clusterConnection.getClusterId()))
                    .map(node -> {
                        this.primaryReplicatingNodes.add(node);
                        ClusterReplicates replicates = this.clusterConfigurationService.getReplicates();
                        int retainedMessageReplicateCount = replicates.getRetainedMessage().getReplicateCount();
                        PrimaryReplicationFilter retainedMessagesFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, retainedMessageReplicateCount);
                        int clientSessionReplicateCount = replicates.getClientSession().getReplicateCount();
                        PrimaryReplicationFilter clientSessionFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, clientSessionReplicateCount);
                        int subscriptionsReplicateCount = replicates.getSubscriptions().getReplicateCount();
                        PrimaryReplicationFilter clientSessionSubscriptionsFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, subscriptionsReplicateCount);
                        int topicTreeReplicateCount = replicates.getTopicTree().getReplicateCount();
                        PrimaryReplicationFilter topicTreeFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, topicTreeReplicateCount);
                        int queuedMessagesReplicateCount = replicates.getQueuedMessages().getReplicateCount();
                        PrimaryReplicationFilter queuedMessagesFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, queuedMessagesReplicateCount);
                        int outgoingMessageFlowReplicateCount = replicates.getOutgoingMessageFlow().getReplicateCount();
                        PrimaryReplicationFilter outgoingMessageFlowFilter = new PrimaryReplicationFilter(this.primaryRing, this.clusterConnection, node, outgoingMessageFlowReplicateCount);
                        return replicate(node, retainedMessagesFilter, clientSessionFilter,
                                clientSessionSubscriptionsFilter, topicTreeFilter,
                                queuedMessagesFilter, outgoingMessageFlowFilter);
                    })
                    .collect(Collectors.toList());
            return ClusterFutures.merge(futures);
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }


    public void sendStateNotification(String node, ClusterState state, ClusterResponse response) {
        List<ClusterRequestFuture<Void, ClusterStateNotificationRequest>> requestFutures = this.clusterConnection.send(new ClusterStateNotificationRequest(node, state), true, Void.class);
        List<ListenableFuture<Void>> futures = requestFutures.stream()
                .map(requestFuture -> requestFuture.setCallback(new ClusterStateNotificationRequestCallback()))
                .collect(Collectors.toList());
        ListenableFuture<Void> future = ClusterFutures.merge(futures);
        LOGGER.debug("Sent {} state notification for {} to all nodes.", state, node);
        ClusterFutures.addCallback(future, new FutureCallback<Void>() {

            @Override
            public void onSuccess(@Nullable Void result) {
                response.sendResult();
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Failed to cast {} state request.", state, t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }

    public void joinMinority(@NotNull String node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        minorityReplication(node, true, true);
    }

    public void c() {
        Set<String> nodes = this.minorityRing.getNodes();
        nodes.forEach(node -> minorityReplication(node, false, true));
    }


    private void minorityReplication(String node, boolean paramBoolean1, boolean paramBoolean2) {
        JChannel jChannel = this.clusterConnection.getJChannel();
        if (jChannel == null ||
                !jChannel.isOpen() ||
                !jChannel.isConnected() ||
                jChannel.isClosed()) {
            return;
        }
        ListenableFuture<?> future = this.clusterExecutor.submit(() -> {
            minorityReplicatingNodes.add(node);
            ClusterReplicates replicates = clusterConfigurationService.getReplicates();

            int retainedMessageReplicateCount = replicates.getRetainedMessage().getReplicateCount();
            MinorityReplicationFilter retainedMessagesFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, retainedMessageReplicateCount, paramBoolean1, paramBoolean2);

            int clientSessionReplicateCount = replicates.getClientSession().getReplicateCount();
            MinorityReplicationFilter clientSessionFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, clientSessionReplicateCount, paramBoolean1, paramBoolean2);

            int subscriptionsReplicateCount = replicates.getSubscriptions().getReplicateCount();
            MinorityReplicationFilter clientSessionSubscriptionsFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, subscriptionsReplicateCount, paramBoolean1, paramBoolean2);

            int topicTreeReplicateCount = replicates.getTopicTree().getReplicateCount();
            MinorityReplicationFilter topicTreeFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, topicTreeReplicateCount, paramBoolean1, paramBoolean2);

            int queuedMessagesReplicateCount = replicates.getQueuedMessages().getReplicateCount();
            MinorityReplicationFilter queuedMessagesFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, queuedMessagesReplicateCount, paramBoolean1, paramBoolean2);

            int outgoingMessageFlowReplicateCount = replicates.getOutgoingMessageFlow().getReplicateCount();
            MinorityReplicationFilter outgoingMessageFlowFilter = new MinorityReplicationFilter(minorityRing, clusterConnection, node, outgoingMessageFlowReplicateCount, paramBoolean1, paramBoolean2);
            replicate(node, retainedMessagesFilter, clientSessionFilter,
                    clientSessionSubscriptionsFilter, topicTreeFilter,
                    queuedMessagesFilter, outgoingMessageFlowFilter);
        });
        ClusterFutures.waitFuture(future);
    }


    private ListenableFuture<Void> replicate(String node,
                                             Filter retainedMessagesFilter,
                                             Filter clientSessionFilter,
                                             Filter clientSessionSubscriptionsFilter,
                                             Filter topicTreeFilter,
                                             Filter queuedMessagesFilter,
                                             Filter outgoingMessageFlowFilter) {
        try {
            List<ListenableFuture<Void>> futures = new ArrayList<>();
            futures.add(this.clusterReplicationService.replicateRetainedMessages(node, retainedMessagesFilter));
            futures.add(this.clusterReplicationService.replicateClientSession(node, clientSessionFilter));
            futures.add(this.clusterReplicationService.replicateClientSessionSubscriptions(node, clientSessionSubscriptionsFilter));
            futures.add(this.clusterReplicationService.replicateTopicTree(node, topicTreeFilter));
            futures.add(this.clusterReplicationService.replicateQueuedMessages(node, queuedMessagesFilter));
            futures.add(this.clusterReplicationService.replicateOutgoingMessageFlow(node, outgoingMessageFlowFilter));
            ListenableFuture<Void> future = ClusterFutures.merge(futures);
            SettableFuture<Void> settableFuture = SettableFuture.create();
            ClusterFutures.addCallback(future, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    primaryReplicatingNodes.remove(node);
                    settableFuture.set(null);
                }

                @Override
                public void onFailure(Throwable t) {
                    LOGGER.error("Error at replication {}", t.getMessage());
                    LOGGER.debug("Original exception ", t);
                    primaryReplicatingNodes.remove(node);
                    minorityReplicatingNodes.add(node);
                    settableFuture.setException(t);
                }
            });
            return settableFuture;
        } catch (Throwable e) {
            return Futures.immediateFailedFuture(e);
        }
    }

    public void removeReplication(@NotNull String node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        ListenableFuture<?> future = this.clusterExecutor.submit(() -> {
            clusterReplicationService.removeRetainedMessagesReplication();
            clusterReplicationService.removeClientSessionReplication();
            clusterReplicationService.removeClientSessionSubscriptionsReplication();
            clusterReplicationService.removeTopicTreeReplication();
            clusterReplicationService.removeQueuedMessagesReplication();
            clusterReplicationService.removeOutgoingMessageFlowReplication();
        });
        ClusterFutures.waitFuture(future);
    }

    public boolean c(String node) {
        return this.primaryReplicatingNodes.contains(node);
    }

    public boolean d(String node) {
        boolean bool = this.minorityReplicatingNodes.contains(node);
        this.minorityReplicatingNodes.remove(node);
        return bool;
    }
}
