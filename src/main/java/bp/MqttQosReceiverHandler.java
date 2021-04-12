package bp;

import bi.CachedMessages;
import bn1.IncomingMessageFlowPersistence;
import bu.InternalPublish;
import cb1.AttributeKeys;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.message.Publish;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

public class MqttQosReceiverHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttQosReceiverHandler.class);
    private final IncomingMessageFlowPersistence incomingMessageFlowPersistence;
    private final Map<Integer, Boolean> forwardFlags;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @Inject
    MqttQosReceiverHandler(IncomingMessageFlowPersistence incomingMessageFlowPersistence) {
        this.incomingMessageFlowPersistence = incomingMessageFlowPersistence;
        this.forwardFlags = new HashMap<>();
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof InternalPublish) {
            handlePublish(ctx, (InternalPublish) msg);
        } else if (msg instanceof PubRel) {
            handlePubRel(ctx, (PubRel) msg);
        } else {
            super.channelRead(ctx, msg);
        }
    }

    private void handlePublish(ChannelHandlerContext ctx, InternalPublish publish) throws Exception {
        switch (publish.getQoS()) {
            case AT_MOST_ONCE:
                super.channelRead(ctx, publish);
                break;
            case AT_LEAST_ONCE:
                super.channelRead(ctx, publish);
                ctx.writeAndFlush(this.cachedMessages.getPubAck(publish.getMessageId()));
                break;
            case EXACTLY_ONCE:
                String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
                int messageId = publish.getMessageId();
                MessageWithId messageWithId = this.incomingMessageFlowPersistence.get(clientId, messageId);
                if (messageWithId == null || !(messageWithId instanceof Publish)) {
                    sentPubRecWhenNotForwarded(ctx, publish, clientId, messageId);
                } else if (publish.isDuplicateDelivery()) {
                    sentPubRecWhenDuplicateDelivery(ctx, publish, clientId);
                } else {
                    startNewFlow(ctx, publish, clientId);
                }
                break;
        }
    }

    private void sentPubRecWhenNotForwarded(ChannelHandlerContext ctx, InternalPublish publish, String clientId, int messageId) throws Exception {
        this.incomingMessageFlowPersistence.addOrReplace(clientId, messageId, publish);
        super.channelRead(ctx, publish);
        this.forwardFlags.put(messageId, true);
        ctx.writeAndFlush(this.cachedMessages.getPubRec(publish.getMessageId()));
        LOGGER.trace("Client {} sent a publish message with id {} which was not forwarded before. This message is processed normally",
                clientId, messageId);
    }

    private void sentPubRecWhenDuplicateDelivery(ChannelHandlerContext ctx, InternalPublish publish, String clientId) throws Exception {
        Boolean forwarded = this.forwardFlags.get(publish.getMessageId());
        if (forwarded != null && forwarded) {
            LOGGER.debug("Client {} sent a duplicate publish message with id {}. This message is ignored",
                    clientId, publish.getMessageId());
        } else {
            super.channelRead(ctx, publish);
            LOGGER.debug("Client {} sent a duplicate publish message with id {} which was not forwarded before. This message is processed normally",
                    clientId, publish.getMessageId());
        }
        this.forwardFlags.put(publish.getMessageId(), true);
        ctx.writeAndFlush(this.cachedMessages.getPubRec(publish.getMessageId()));
    }

    private void startNewFlow(ChannelHandlerContext ctx, InternalPublish publish, String clientId) throws Exception {
        LOGGER.debug("Client {} sent a new Publish with QoS 2 and a message identifier which is already in process ({}) by another flow! Starting new flow",
                clientId, publish.getMessageId());
        this.incomingMessageFlowPersistence.addOrReplace(clientId, publish.getMessageId(), publish);
        super.channelRead(ctx, publish);
        this.forwardFlags.put(publish.getMessageId(), true);
        ctx.writeAndFlush(this.cachedMessages.getPubRec(publish.getMessageId()));
    }

    private void handlePubRel(ChannelHandlerContext ctx, PubRel pubRel) {
        String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
        int messageId = pubRel.getMessageId();
        this.incomingMessageFlowPersistence.addOrReplace(clientId, messageId, pubRel);
        ctx.writeAndFlush(this.cachedMessages.getPubComp(messageId))
                .addListener(new PubCompSendCompletedListener(messageId, clientId,
                        this.forwardFlags, this.incomingMessageFlowPersistence));
    }

    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Boolean persistentSession = ctx.channel().attr(AttributeKeys.MQTT_PERSISTENT_SESSION).get();
        if (persistentSession != null && !persistentSession) {
            String clientId = ctx.channel().attr(AttributeKeys.MQTT_CLIENT_ID).get();
            if (clientId != null) {
                this.incomingMessageFlowPersistence.removeAll(clientId);
            }
        }
        super.channelInactive(ctx);
    }

    private static class PubCompSendCompletedListener implements ChannelFutureListener {
        private final int messageId;
        private final String clientId;
        private final Map<Integer, Boolean> forwardFlags;
        private final IncomingMessageFlowPersistence incomingMessageFlowPersistence;

        public PubCompSendCompletedListener(int messageId, String clientId, Map<Integer, Boolean> forwardFlags, IncomingMessageFlowPersistence incomingMessageFlowPersistence) {
            this.messageId = messageId;
            this.clientId = clientId;
            this.forwardFlags = forwardFlags;
            this.incomingMessageFlowPersistence = incomingMessageFlowPersistence;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            if (future.isSuccess()) {
                this.forwardFlags.remove(this.messageId);
                this.incomingMessageFlowPersistence.remove(this.clientId, this.messageId);
            }
        }
    }
}
