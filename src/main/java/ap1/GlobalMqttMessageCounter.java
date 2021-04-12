package ap1;

import am1.Metrics;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.Disconnect;
import com.hivemq.spi.message.Message;
import com.hivemq.spi.message.PingReq;
import com.hivemq.spi.message.PingResp;
import com.hivemq.spi.message.PubAck;
import com.hivemq.spi.message.PubComp;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.message.SubAck;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.message.UnsubAck;
import com.hivemq.spi.message.Unsubscribe;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;

import javax.inject.Inject;
import javax.inject.Singleton;

@ChannelHandler.Sharable
@Singleton
public class GlobalMqttMessageCounter extends ChannelDuplexHandler {
    private final Metrics metrics;

    @Inject
    public GlobalMqttMessageCounter(Metrics metrics) {
        this.metrics = metrics;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof Message)) {
            super.channelRead(ctx, msg);
            return;
        }
        this.metrics.incomingMessageCount().inc();
        this.metrics.incomingMessageRate().mark();
        if (msg instanceof Connect) {
            this.metrics.incomingConnectCount().inc();
            this.metrics.incomingConnectRate().mark();
        }
        if (msg instanceof Disconnect) {
            this.metrics.incomingDisconnectCount().inc();
            this.metrics.incomingDisconnectRate().mark();
        }
        if (msg instanceof PingReq) {
            this.metrics.incomingPingReqCount().inc();
            this.metrics.incomingPingReqRate().mark();
        }
        if (msg instanceof PubAck) {
            this.metrics.incomingPubAckRate().mark();
            this.metrics.incomingPubAckCount().inc();
        }
        if (msg instanceof PubComp) {
            this.metrics.incomingPubCompRate().mark();
            this.metrics.incomingPubCompCount().inc();
        }
        if (msg instanceof Publish) {
            this.metrics.incomingPublishRate().mark();
            this.metrics.incomingPublishCount().inc();
        }
        if (msg instanceof PubRec) {
            this.metrics.incomingPubRecRate().mark();
            this.metrics.incomingPubRecCount().inc();
        }
        if (msg instanceof PubRel) {
            this.metrics.incomingPubRelRate().mark();
            this.metrics.incomingPubRelCount().inc();
        }
        if (msg instanceof Subscribe) {
            this.metrics.incomingSubscribeRate().mark();
            this.metrics.incomingSubscribeCount().inc();
        }
        if (msg instanceof Unsubscribe) {
            this.metrics.incomingUnsubscribeRate().mark();
            this.metrics.incomingUnsubscribeCount().inc();
        }
        super.channelRead(ctx, msg);
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof Message)) {
            super.write(ctx, msg, promise);
            return;
        }
        this.metrics.outgoingMessageCount().inc();
        this.metrics.outgoingMessageRate().mark();
        if (msg instanceof ConnAck) {
            this.metrics.outgoingConnAckCount().inc();
            this.metrics.outgoingConnAckRate().mark();
        }
        if (msg instanceof PingResp) {
            this.metrics.outgoingPingRespCount().inc();
            this.metrics.outgoingPingRespRate().mark();
        }
        if (msg instanceof PubAck) {
            this.metrics.outgoingPubAckRate().mark();
            this.metrics.outgoingPubAckCount().inc();
        }
        if (msg instanceof PubComp) {
            this.metrics.outgoingPubCompRate().mark();
            this.metrics.outgoingPubCompCount().inc();
        }
        if (msg instanceof Publish) {
            this.metrics.outgoingPublishRate().mark();
            this.metrics.outgoingPublishCount().inc();
        }
        if (msg instanceof PubRec) {
            this.metrics.outgoingPubRecRate().mark();
            this.metrics.outgoingPubRecCount().inc();
        }
        if (msg instanceof PubRel) {
            this.metrics.outgoingPubRelRate().mark();
            this.metrics.outgoingPubRelCount().inc();
        }
        if (msg instanceof SubAck) {
            this.metrics.outgoingSubAckRate().mark();
            this.metrics.outgoingSubAckCount().inc();
        }
        if (msg instanceof UnsubAck) {
            this.metrics.outgoingUnsubAckRate().mark();
            this.metrics.outgoingUnsubAckCount().inc();
        }
        super.write(ctx, msg, promise);

    }
}
