package t;

import a1.ClusterRequestDispatcher;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import ac.SerializationService;
import ad.SerializationException;
import ah.ClusterService;
import ah.ClusterState;
import ah.ClusterStateService;
import aj.ClusterFutures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import d.CacheScoped;
import j1.ClusterRequest;
import org.jgroups.Address;
import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;
import org.jgroups.View;
import org.jgroups.blocks.AsyncRequestHandler;
import org.jgroups.blocks.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Cluster;
import s.Minority;
import s.Primary;

import javax.inject.Inject;
import javax.inject.Provider;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@CacheScoped
public class ClusterReceiverService extends ReceiverAdapter
        implements AsyncRequestHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterReceiverService.class);
    private final ConsistentHashingRing minorityRing;
    private final ConsistentHashingRing primaryRing;
    private final SerializationService serializationService;
    private final Provider<ClusterRequestDispatcher> clusterRequestDispatcherProvider;
    private final ClusterService clusterService;
    private final ClusterConnection clusterConnection;
    private final ClusterStateService clusterStateService;
    private final ListeningExecutorService clusterExecutor;
    private View clusterView;


    @Inject
    ClusterReceiverService(@Minority ConsistentHashingRing minorityRing,
                           @Primary ConsistentHashingRing primaryRing,
                           SerializationService serializationService,
                           Provider<ClusterRequestDispatcher> clusterRequestDispatcherProvider,
                           ClusterService clusterService,
                           ClusterConnection clusterConnection,
                           ClusterStateService clusterStateService,
                           @Cluster ListeningExecutorService clusterExecutor) {
        this.minorityRing = minorityRing;
        this.primaryRing = primaryRing;
        this.serializationService = serializationService;
        this.clusterRequestDispatcherProvider = clusterRequestDispatcherProvider;
        this.clusterService = clusterService;
        this.clusterConnection = clusterConnection;
        this.clusterStateService = clusterStateService;
        this.clusterExecutor = clusterExecutor;
    }

    public void viewAccepted(View view) {
        LOGGER.info("Cluster size = {}, members : {}.", view.size(), view.getMembers());
        ListenableFuture future = this.clusterExecutor
                .submit(new ClusterViewAcceptedTask(this.clusterView, view, this.clusterService));
        ClusterFutures.waitFuture(future);
        this.clusterView = view;
    }

    public void suspect(Address address) {
        LOGGER.debug("Suspected node : {}.", address);
    }

    public void handle(Message message, Response response) {
        try {
            Object object = this.serializationService.deserialize(message.getBuffer());
            Address address = message.getSrc();
            String node = this.clusterConnection.getJChannel().getName(address);
            if (object instanceof ClusterRequest) {
                ClusterRequest request = (ClusterRequest) object;
                this.clusterRequestDispatcherProvider.get().dispatch(request,
                        new ClusterResponse(this.serializationService, this.clusterConnection, response), node);
            } else if (object == null) {
                LOGGER.warn("Cluster received null object.");
            } else {
                LOGGER.warn("Received a cluster message that is not a request. It will be ignored. Type = {}", object.getClass());
            }
        } catch (SerializationException e) {
            LOGGER.error("Exception during deserialization of a request ", e);
            new ClusterResponse(this.serializationService, this.clusterConnection, response).sendResult(ClusterResponseCode.FAILED);
        }
    }

    public Object handle(Message message) {
        throw new UnsupportedOperationException("Synchronous request handling should not be used. Set message dispatcher to async dispatching.");
    }

    public void setState(InputStream input) throws SerializationException {
        try {
            Map<String, ClusterState> clusterNodeStates = this.serializationService.deserialize(input, HashMap.class);
            clusterNodeStates.forEach((node, state) -> {
                this.clusterStateService.change(node, state);
                if (state == ClusterState.JOINING || state == ClusterState.MERGING) {
                    this.minorityRing.add(node);
                } else if (state == ClusterState.RUNNING) {
                    this.minorityRing.add(node);
                    this.primaryRing.add(node);
                }
            });
        } catch (SerializationException e) {
            LOGGER.error("Exception during deserialization of initial cluster state.", e);
            throw e;
        }
    }

    public void getState(OutputStream output) {
        this.serializationService.serialize(this.clusterStateService.getClusterNodeStates(), output);
    }
}
