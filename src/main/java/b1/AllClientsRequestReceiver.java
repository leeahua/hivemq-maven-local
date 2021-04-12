package b1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.AllClientsRequest;
import v.ClientSessionClusterPersistence;

import javax.annotation.Nullable;
import java.util.Set;

@CacheScoped
public class AllClientsRequestReceiver implements ClusterReceiver<AllClientsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AllClientsRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;

    @Inject
    public AllClientsRequestReceiver(ClientSessionClusterPersistence clientSessionClusterPersistence) {
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
    }

    public void received(@NotNull AllClientsRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received all clients request from {}.", sender);
        ListenableFuture<Set<String>> future = this.clientSessionClusterPersistence.getLocalAllClients();
        ClusterFutures.addCallback(future, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                response.sendResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while getting all clients for request.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
