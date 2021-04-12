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
import com.hivemq.spi.services.AsyncClientService;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ConnectedClientsRequest;

import javax.annotation.Nullable;
import java.util.Set;

@CacheScoped
public class ConnectedClientsRequestReceiver
        implements ClusterReceiver<ConnectedClientsRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedClientsRequestReceiver.class);
    private final AsyncClientService asyncClientService;

    @Inject
    public ConnectedClientsRequestReceiver(AsyncClientService asyncClientService) {
        this.asyncClientService = asyncClientService;
    }

    public void received(@NotNull ConnectedClientsRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received connected clients request from {}.", sender);
        ListenableFuture<Set<String>> future = this.asyncClientService.getLocalConnectedClients();
        ClusterFutures.addCallback(future, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                response.sendResult(request);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while getting connected clients for request.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
