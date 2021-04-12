package bj;

import cb1.ChannelUtils;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.Message;
import com.hivemq.spi.message.ReturnCode;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class MqttMessageBarrier extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageBarrier.class);
    private final AtomicBoolean connectAlreadySent = new AtomicBoolean(false);
    private final AtomicBoolean connAckAlreadySent = new AtomicBoolean(false);
    private final Queue<Message> messageQueue = new LinkedList<>();

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Connect) {
            this.connectAlreadySent.set(true);
        } else if (msg instanceof Message) {
            if (!this.connectAlreadySent.get()) {
                ctx.channel().close();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Disconnecting client with IP [{}] because it sent another message before a Connect message",
                            ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                }
                return;
            }
            if (this.connectAlreadySent.get() && !this.connAckAlreadySent.get()) {
                this.messageQueue.add((Message) msg);
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ConnAck) {
            ConnAck connAck = (ConnAck) msg;
            if (connAck.getReturnCode() == ReturnCode.ACCEPTED) {
                promise.addListener(new ChannelFutureListener() {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if (future.isSuccess()) {
                            removeSelf(ctx);
                            connAckAlreadySent.set(true);
                            releaseQueuedMessages(ctx);
                        }
                    }
                });
            }
        }
        super.write(ctx, msg, promise);
    }

    private void removeSelf(ChannelHandlerContext ctx) {
        ctx.pipeline().remove(this);
    }

    private void releaseQueuedMessages(ChannelHandlerContext ctx) {
        this.messageQueue.forEach(ctx.pipeline()::fireChannelRead);
    }

    @VisibleForTesting
    protected boolean isConnectAlreadySent() {
        return this.connectAlreadySent.get();
    }

    @VisibleForTesting
    protected Collection<Message> getQueuedMessages() {
        return Collections.unmodifiableCollection(this.messageQueue);
    }
}
