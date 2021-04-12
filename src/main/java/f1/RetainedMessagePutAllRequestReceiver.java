package f1;

import a1.ClusterReceiver;
import a1.GenericRequestReceiver;
import ab.ClusterResponse;
import aj.ClusterFutures;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import j1.ClusterReplicateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import s.Rpc;
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageAddReplicateRequest;
import x1.RetainedMessagePutAllRequest;
import x1.RetainedMessageRemoveReplicateRequest;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@CacheScoped
public class RetainedMessagePutAllRequestReceiver
        extends GenericRequestReceiver
        implements ClusterReceiver<RetainedMessagePutAllRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessagePutAllRequestReceiver.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;

    @Inject
    RetainedMessagePutAllRequestReceiver(
            RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getQueuedMessages().getReplicateCount(), rpcExecutor);
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
    }

    public void received(RetainedMessagePutAllRequest request, ClusterResponse response, String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        ImmutableSet<ClusterReplicateRequest> requests = request.getRequests();
        this.LOGGER.trace("Received retained message PUT ALL request.");
        List<ListenableFuture<Void>> futures = requests.stream()
                .map(r -> {
                    if (r instanceof RetainedMessageAddReplicateRequest) {
                        RetainedMessageAddReplicateRequest addRequest = (RetainedMessageAddReplicateRequest) r;
                        return this.retainedMessagesClusterPersistence.persist(addRequest.getTopic(), addRequest.getRetainedMessage(), addRequest.getVectorClock(), addRequest.getTimestamp());
                    }
                    if (r instanceof RetainedMessageRemoveReplicateRequest) {
                        RetainedMessageRemoveReplicateRequest removeRequest = (RetainedMessageRemoveReplicateRequest) r;
                        this.retainedMessagesClusterPersistence.persist(removeRequest.getTopic(), null, removeRequest.getVectorClock(), removeRequest.getTimestamp());
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        sendResult(ClusterFutures.merge(futures), response);
    }

    protected void onFailure(Throwable t) {
        this.LOGGER.error("Exception while processing retained message put all request.", t);
    }
}
