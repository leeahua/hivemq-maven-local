package b1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import bl1.ChannelPersistence;
import cb1.AttributeKeys;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import d.CacheScoped;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import t1.ClientTakeoverRequest;

@CacheScoped
public class ClientTakeoverRequestReceiver
        implements ClusterReceiver<ClientTakeoverRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientTakeoverRequestReceiver.class);
    private final ChannelPersistence channelPersistence;

    @Inject
    public ClientTakeoverRequestReceiver(ChannelPersistence channelPersistence) {
        this.channelPersistence = channelPersistence;
    }

    public void received(@NotNull ClientTakeoverRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        LOGGER.trace("Received client takeover request for client {}.", request.getClientId());
        disconnectAlreadyConnectedClient(request.getClientId());
        response.sendResult();
    }

    private void disconnectAlreadyConnectedClient(String clientId) {
        Channel channel = this.channelPersistence.getChannel(clientId);
        if (channel != null) {
            LOGGER.debug("Disconnecting already connected client with id {} because another client connects with that id", clientId);
            channel.attr(AttributeKeys.MQTT_TAKEN_OVER).set(true);
            channel.close();
        }
    }
}
