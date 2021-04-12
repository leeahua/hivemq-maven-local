package a1;

import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import j1.ClusterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

@CacheScoped
public class ClusterRequestDispatcherImpl implements ClusterRequestDispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterRequestDispatcherImpl.class);
    protected final ListeningExecutorService rpcExecutor;
    private final Map<Class<? extends ClusterRequest>, ClusterReceiver<?>> receivers;

    public ClusterRequestDispatcherImpl(
            Map<Class<? extends ClusterRequest>, ClusterReceiver<?>> receivers,
            ListeningExecutorService rpcExecutor) {
        this.receivers = receivers;
        this.rpcExecutor = rpcExecutor;
    }

    public void dispatch(@NotNull ClusterRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        Class<? extends ClusterRequest> requestType = request.getClass();
        ClusterReceiver receiver = (ClusterReceiver) this.receivers.get(requestType);
        if (receiver == null) {
            LOGGER.warn("There is no receiver registered for a request of type: " + request.getClass() + ".");
            response.sendResult(ClusterResponseCode.FAILED);
            return;
        }
        ListenableFuture<?> future = this.rpcExecutor.submit(
                () -> {
                    try {
                        receiver.received(request, response, sender);
                    } catch (Throwable e) {
                        LOGGER.error("Exception during {} handling.", request.getClass(), e);
                        response.sendResult(ClusterResponseCode.FAILED);
                    }
                });
        ClusterFutures.waitFuture(future);
    }

    public Map<Class<? extends ClusterRequest>, ClusterReceiver<?>> getReceivers() {
        return this.receivers;
    }
}
