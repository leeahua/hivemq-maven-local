package g1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import ah.ClusterContext;
import ah.ClusterService;
import ah.ClusterState;
import ah.ClusterStateService;
import ah.Condition;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import j1.ClusterRequestFuture;
import k1.ClusterCallback;
import k1.ClusterCallbackFactoryImpl;
import k1.DefaultClusterCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q1.ClusterContextRequestCallback;
import q1.NodeInformationRequestCallback;
import t.ClusterConnection;
import y1.ClusterContextRequest;
import y1.ClusterStateRequest;
import y1.LicenseCheckRequest;
import y1.LicenseInformationRequest;
import y1.LicenseInformationResult;
import y1.NodeInformationRequest;
import y1.ReplicateFinishedRequest;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

// TODO:
@CacheScoped
public class ClusterStateRequestReceiver
        implements ClusterReceiver<ClusterStateRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterStateRequestReceiver.class);
    private final ClusterConnection clusterConnection;
    private final ClusterService clusterService;
    private final ClusterStateService clusterStateService;
    private final ClusterConfigurationService clusterConfigurationService;
    private final ClusterContext clusterContext;

    @Inject
    ClusterStateRequestReceiver(ClusterConnection clusterConnection,
                                ClusterService clusterService,
                                ClusterStateService clusterStateService,
                                ClusterConfigurationService clusterConfigurationService,
                                ClusterContext clusterContext) {
        this.clusterConnection = clusterConnection;
        this.clusterService = clusterService;
        this.clusterStateService = clusterStateService;
        this.clusterConfigurationService = clusterConfigurationService;
        this.clusterContext = clusterContext;
    }

    public void received(@NotNull ClusterStateRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        ClusterState state = request.getState();
        if (state == this.clusterStateService.getState(sender)) {
            LOGGER.trace("Received {} state request, from {}, state is already {}",
                    state, sender, state);
            response.sendResult();
            return;
        }
        LOGGER.trace("Received {} state request, from {}.", state, sender);
        switch (state) {
            case UNKNOWN:
                ClusterRequestFuture<ImmutableMap, ClusterContextRequest> contextRequestFuture =
                        this.clusterConnection.send(new ClusterContextRequest(this.clusterContext.getNodeInformations()),
                                sender, ImmutableMap.class);
                ListenableFuture<ImmutableMap> contextFuture = contextRequestFuture.setCallback(new ClusterContextRequestCallback());
                ClusterFutures.addCallback(contextFuture, new FutureCallback<ImmutableMap>() {

                    @Override
                    public void onSuccess(@Nullable ImmutableMap result) {
                        if (!sameConfig(result)) {
                            response.sendResult(ClusterResponseCode.FAILED, null, false);
                            return;
                        }
                        String version = (String) result.get("hivemq.version");
                        clusterContext.setVersion(sender, version);
                        ListenableFuture<Void> nodeInformationFuture = sendNodeInformationRequest(sender, version);
                        ClusterFutures.addCallback(nodeInformationFuture, new FutureCallback<Void>() {

                            @Override
                            public void onSuccess(@Nullable Void result) {
                                clusterService.sendStateNotification(sender, state, response);
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                LOGGER.warn("Node information request failed.", t);
                                response.sendResult(ClusterResponseCode.FAILED);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOGGER.warn("Cluster context request failed.", t);
                        response.sendResult(ClusterResponseCode.FAILED);
                    }
                });
                break;
            case NOT_JOINED:
                this.clusterService.sendStateNotification(sender, state, response);
                break;
            case JOINING:
                ClusterRequestFuture<LicenseInformationResult, LicenseInformationRequest> licenseRequestFuture =
                        this.clusterConnection.send(new LicenseInformationRequest(), sender, LicenseInformationResult.class);
                ListenableFuture<LicenseInformationResult> licenseResultFuture = licenseRequestFuture.setCallback(new DefaultClusterCallback(LicenseInformationResult.class));
                ClusterFutures.addCallback(licenseResultFuture, new FutureCallback<LicenseInformationResult>() {

                    @Override
                    public void onSuccess(@Nullable LicenseInformationResult result) {
                        AtomicBoolean validLicense = new AtomicBoolean(true);
                        LicenseCheckRequest checkRequest = new LicenseCheckRequest(result.getLicenseState(), result.getLicenseId());
                        ListenableFuture<Void> checkFuture = clusterConnection.send(checkRequest, Boolean.class,
                                new ClusterCallbackFactoryImpl(Boolean.class), false,
                                (node, value) -> {
                                    if (!value) {
                                        validLicense.set(false);
                                    }
                                });
                        ClusterFutures.addCallback(checkFuture, new FutureCallback<Void>() {

                            @Override
                            public void onSuccess(@Nullable Void result) {
                                if (validLicense.get()) {
                                    a(response, sender, state);
                                } else {
                                    response.sendResult(ClusterResponseCode.FAILED, null, false);
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                response.sendResult(ClusterResponseCode.FAILED);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        response.sendResult(ClusterResponseCode.FAILED);
                    }
                });
                break;
            case RUNNING:
                a(response, sender, state);
                break;
            case MERGE_MINORITY:
                this.clusterService.sendStateNotification(sender, state, response);
                break;
            case SHUTTING_DOWN:
                break;
            case SHUTDOWN_FINISHED:
                break;
            case DEAD:
                break;
            default:
                throw new IllegalStateException("Received unknown cluster state request.");
        }
    }


    private void a(ClusterResponse response, String node, ClusterState state) {
        Set<String> localSet = this.clusterStateService.getNodes(ClusterState.RUNNING, ClusterState.SHUTTING_DOWN);
        ArrayList localArrayList = new ArrayList();
        Object localObject = localSet.iterator();
        while (((Iterator) localObject).hasNext()) {
            String str = (String) ((Iterator) localObject).next();
            ClusterRequestFuture locale = this.clusterConnection.send(new ReplicateFinishedRequest(), str, Boolean.class);
            localArrayList.add(locale.setCallback(new ReplicateFinishedRequestCallback()));
        }
        localObject = Futures.allAsList(localArrayList);
        ClusterFutures.addCallback((ListenableFuture) localObject, new a(response, node, state));
    }


    private ListenableFuture<Void> sendNodeInformationRequest(String node, String version) {
        List<ClusterRequestFuture<Void, NodeInformationRequest>> requestFutures = this.clusterConnection.send(new NodeInformationRequest(node, version), false, Void.class);
        List<ListenableFuture<Void>> futures =
        requestFutures.stream()
                .map(requestFuture->
                        requestFuture.setCallback(new NodeInformationRequestCallback()))
                .collect(Collectors.toList());
        return ClusterFutures.merge(futures);
    }

    private boolean sameConfig(ImmutableMap<String, Object> nodeInformations) {
        if (!nodeInformations.containsKey("transport.type") ||
                !nodeInformations.containsKey("discovery.type")) {
            return false;
        }
        if (!this.clusterConfigurationService.getTransport().getType()
                .equals(nodeInformations.get("transport.type"))) {
            return false;
        }
        return this.clusterConfigurationService.getDiscovery().getType()
                .equals(nodeInformations.get("discovery.type"));
    }

    private class a implements FutureCallback<List<Boolean>> {
        private final ClusterResponse response;
        private final String node;
        private final ClusterState state;

        public a(ClusterResponse response, String node, ClusterState state) {
            this.response = response;
            this.node = node;
            this.state = state;
        }

        @Override
        public void onSuccess(@Nullable List<Boolean> result) {
            if (result.contains(false)) {
                this.response.sendResult(ClusterResponseCode.DENIED);
            } else if (clusterStateService.change(this.node, this.state,
                    Condition.allNotEqual(ClusterState.JOINING)
                            .and(Condition.allNotEqual(ClusterState.SHUTTING_DOWN))
                            .and(Condition.allNotEqual(ClusterState.MERGING)))) {
                clusterService.sendStateNotification(this.node, this.state, this.response);
            } else {
                this.response.sendResult(ClusterResponseCode.DENIED);
            }
        }

        public void onFailure(Throwable paramThrowable) {
            this.response.sendResult(ClusterResponseCode.FAILED);
        }
    }

    private class ReplicateFinishedRequestCallback extends ClusterCallback<Boolean, ReplicateFinishedRequest> {
        private ReplicateFinishedRequestCallback() {
        }

        public void onSuccess(Boolean result) {
            this.settableFuture.set(result);
        }

        public void onDenied() {
            this.settableFuture.set(false);
        }

        public void onNotResponsible() {
            this.settableFuture.set(false);
        }

        public void onSuspected() {
            this.settableFuture.set(false);
        }

        public void onFailure(Throwable t) {
            this.settableFuture.setException(t);
        }

        public void onTimedOut() {
            retryAndIncreaseTimeout(Boolean.class);
        }

        public void onBusy() {
            retry(Boolean.class);
        }
    }
}
