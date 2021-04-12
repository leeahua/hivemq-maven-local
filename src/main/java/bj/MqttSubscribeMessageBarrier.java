package bj;

import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.message.Message;
import com.hivemq.spi.message.PingReq;
import com.hivemq.spi.message.SubAck;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.message.UnsubAck;
import com.hivemq.spi.message.Unsubscribe;
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

public class MqttSubscribeMessageBarrier extends ChannelDuplexHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttSubscribeMessageBarrier.class);
    private final AtomicBoolean processing = new AtomicBoolean(false);
    private final Queue<Message> backlogMessages = new LinkedList<>();

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message && !(msg instanceof PingReq)) {
            if ((msg instanceof Subscribe || msg instanceof Unsubscribe) &&
                    !this.processing.get()) {
                this.processing.set(true);
                ctx.channel().config().setAutoRead(false);
                super.channelRead(ctx, msg);
                return;
            }
            if (this.processing.get()) {
                this.backlogMessages.add((Message) msg);
                return;
            }
        }
        super.channelRead(ctx, msg);
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof SubAck || msg instanceof UnsubAck) {
            promise.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        boolean isProcessing = isProcessing(ctx);
                        processing.set(!isProcessing);
                        ctx.channel().config().setAutoRead(isProcessing);
                    }
                }
            });
        }
        super.write(ctx, msg, promise);
    }

    private boolean isProcessing(ChannelHandlerContext ctx) {
        while (this.backlogMessages.size() > 0) {
            Message message = this.backlogMessages.poll();
            ctx.fireChannelRead(message);
            if (message instanceof Subscribe ||
                    message instanceof Unsubscribe) {
                return false;
            }
        }
        return true;
    }

    @VisibleForTesting
    protected boolean isProcessing() {
        return this.processing.get();
    }

    @VisibleForTesting
    protected Collection<Message> getBacklogMessages() {
        return Collections.unmodifiableCollection(this.backlogMessages);
    }
}
