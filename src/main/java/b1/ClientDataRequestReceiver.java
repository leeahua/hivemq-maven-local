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
import com.hivemq.spi.security.ClientData;
import com.hivemq.spi.services.AsyncClientService;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientDataRequest;

import javax.annotation.Nullable;

@CacheScoped
public class ClientDataRequestReceiver
        implements ClusterReceiver<ClientDataRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectedClientsRequestReceiver.class);
    private final AsyncClientService asyncClientService;
    
    @Inject
    public ClientDataRequestReceiver(AsyncClientService asyncClientService) {
        this.asyncClientService = asyncClientService;
    }

    public void received(@NotNull ClientDataRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received connected clients request from {}.", sender);
        ListenableFuture<ClientData> future = this.asyncClientService.getLocalClientData(request.getClientId());
        ClusterFutures.addCallback(future, new FutureCallback<ClientData>(){

            @Override
            public void onSuccess(@Nullable ClientData result) {
                response.sendResult(result);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while getting client data for request.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        });
    }
}
