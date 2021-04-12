package f1;

import a1.AbstractRequestReceiver;
import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import bz.RetainedMessage;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Minority;
import s.Primary;
import s.Rpc;
import x.RetainedMessagesClusterPersistence;
import x1.RetainedMessageGetRequest;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

@CacheScoped
public class RetainedMessageGetRequestReceiver
        extends AbstractRequestReceiver
        implements ClusterReceiver<RetainedMessageGetRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RetainedMessageGetRequestReceiver.class);
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;
    private final ExecutorService rpcExecutor;

    @Inject
    RetainedMessageGetRequestReceiver(
            RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
            @Rpc ExecutorService rpcExecutor,
            @Primary ConsistentHashingRing primaryRing,
            @Minority ConsistentHashingRing minorityRing,
            ClusterIdProducer clusterIdProducer,
            ClusterConfigurationService clusterConfigurationService) {
        super(primaryRing, minorityRing, clusterIdProducer, clusterConfigurationService.getReplicates().getClientSession().getReplicateCount());
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
        this.rpcExecutor = rpcExecutor;
    }

    public void received(@NotNull RetainedMessageGetRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received retained message GET request for topic {}", request.getTopic());
        if (!request.getTopic().contains("+") && !request.getTopic().contains("#")) {
            if (!readable(request.getKey(), response, RetainedMessage.class)) {
                return;
            }
            RetainedMessage retainedMessage = this.retainedMessagesClusterPersistence.getLocally(request.getTopic());
            response.sendResult(retainedMessage);
            return;
        }
        ImmutableSet<ListenableFuture<Set<String>>> futures =
                this.retainedMessagesClusterPersistence.getLocalWithWildcards(request.getTopic());
        Futures.addCallback(Futures.allAsList(futures), new FutureCallback<List<Set<String>>>() {

            @Override
            public void onSuccess(@Nullable List<Set<String>> result) {
                if (result == null) {
                    response.sendResult(ClusterResponseCode.OK, Collections.emptySet());
                }
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                builder.addAll(result.stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toList()));
                response.sendResult(ClusterResponseCode.OK, builder.build());
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Exception while processing retained message get request.", t);
                response.sendResult(ClusterResponseCode.FAILED);
            }
        }, this.rpcExecutor);
    }
}
