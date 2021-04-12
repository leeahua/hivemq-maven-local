package e1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import bl1.ChannelPersistence;
import bn1.OutgoingMessageFlowClusterPersistence;
import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import i.ClusterIdProducer;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Primary;
import w1.ClusterPubRelRequest;

import javax.inject.Inject;

@CacheScoped
public class ClusterPubRelRequestReceiver
        implements ClusterReceiver<ClusterPubRelRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPubRelRequestReceiver.class);
    private final ClusterIdProducer clusterIdProducer;
    private final ConsistentHashingRing primaryRing;
    private final ChannelPersistence channelPersistence;
    private final MessageIDPools messageIDPools;
    private final OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence;

    @Inject
    public ClusterPubRelRequestReceiver(ClusterIdProducer clusterIdProducer,
                                        @Primary ConsistentHashingRing primaryRing,
                                        ChannelPersistence channelPersistence,
                                        MessageIDPools messageIDPools,
                                        OutgoingMessageFlowClusterPersistence outgoingMessageFlowClusterPersistence) {
        this.clusterIdProducer = clusterIdProducer;
        this.primaryRing = primaryRing;
        this.channelPersistence = channelPersistence;
        this.messageIDPools = messageIDPools;
        this.outgoingMessageFlowClusterPersistence = outgoingMessageFlowClusterPersistence;
    }

    public void received(@NotNull ClusterPubRelRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received cluster PubRel for client '{}' from node {}.", request.getClientId(), sender);
        boolean sameMessageId;
        try {
            int messageId = this.messageIDPools.forClient(request.getClientId()).next(request.getPubRel().getMessageId());
            sameMessageId = messageId == request.getPubRel().getMessageId();
        } catch (ProduceMessageIdException e) {
            sameMessageId = false;
        }
        if (!sameMessageId) {
            response.sendResult();
            return;
        }
        Channel channel = this.channelPersistence.getChannel(request.getClientId());
        if (channel != null) {
            channel.writeAndFlush(request.getPubRel());
        } else if (this.primaryRing.getNode(request.getClientId()).equals(this.clusterIdProducer.get())) {
            this.outgoingMessageFlowClusterPersistence.add(request.getClientId(), request.getPubRel());
        } else {
            response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE);
        }
    }
}
