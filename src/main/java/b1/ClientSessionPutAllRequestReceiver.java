package b1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientSessionPutAllRequest;
import v.ClientSessionClusterPersistence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.Collectors;

@CacheScoped
public class ClientSessionPutAllRequestReceiver implements ClusterReceiver<ClientSessionPutAllRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionPutAllRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    ClientSessionPutAllRequestReceiver(ClientSessionClusterPersistence clientSessionClusterPersistence) {
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull ClientSessionPutAllRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client session put all request from node {}.", sender);
        List<ListenableFuture<Void>> mergeFutures =
                request.getRequests().stream()
                        .map(r -> this.clientSessionClusterPersistence.handleMergeReplica(r.getClientId(), r.getClientSession(), r.getTimestamp()))
                        .collect(Collectors.toList());
        ClusterFutures.addCallback(ClusterFutures.merge(mergeFutures),
                new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable Void result) {
                        response.sendResult();
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        LOGGER.error("Exception at ClientSessionPutAllRequest", t);
                        response.sendResult();
                    }
                }
        );
    }
}
