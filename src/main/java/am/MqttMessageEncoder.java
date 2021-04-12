package am;

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
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MqttMessageEncoder extends MessageToByteEncoder<Message> {
    public static final ConnAckMessageEncoder CONN_ACK_MESSAGE_ENCODER = new ConnAckMessageEncoder();
    public static final PingRespMessageEncoder PING_RESP_MESSAGE_ENCODER = new PingRespMessageEncoder();
    public static final PingReqMessageEncoder PING_REQ_MESSAGE_ENCODER = new PingReqMessageEncoder();
    public static final PubAckMessageEncoder PUB_ACK_MESSAGE_ENCODER = new PubAckMessageEncoder();
    public static final PubRecMessageEncoder PUB_REC_MESSAGE_ENCODER = new PubRecMessageEncoder();
    public static final PubRelMessageEncoder PUB_REL_MESSAGE_ENCODER = new PubRelMessageEncoder();
    public static final PubCompMessageEncoder PUB_COMP_MESSAGE_ENCODER = new PubCompMessageEncoder();
    public static final SubAckMessageEncoder SUB_ACK_MESSAGE_ENCODER = new SubAckMessageEncoder();
    public static final UnsubAckMessageEncoder UNSUB_ACK_MESSAGE_ENCODER = new UnsubAckMessageEncoder();
    public static final PublishMessageEncoder PUBLISH_MESSAGE_ENCODER = new PublishMessageEncoder();
    public static final SubscribeMessageEncoder SUBSCRIBE_MESSAGE_ENCODER = new SubscribeMessageEncoder();
    public static final UnsubscribeMessageEncoder UNSUBSCRIBE_MESSAGE_ENCODER = new UnsubscribeMessageEncoder();
    public static final DisconnectMessageEncoder DISCONNECT_MESSAGE_ENCODER = new DisconnectMessageEncoder();
    public static final ConnectMessageEncoder CONNECT_MESSAGE_ENCODER = new ConnectMessageEncoder();
    private final Metrics metrics;

    public MqttMessageEncoder(Metrics metrics) {
        this.metrics = metrics;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
        if (msg instanceof Publish) {
            PUBLISH_MESSAGE_ENCODER.encode(ctx, (Publish) msg, out);
            this.metrics.outgoingPublishSizeMean().update(out.writerIndex());
        } else if (msg instanceof PingReq) {
            PING_REQ_MESSAGE_ENCODER.encode(ctx, (PingReq) msg, out);
        } else if (msg instanceof PingResp) {
            PING_RESP_MESSAGE_ENCODER.encode(ctx, (PingResp) msg, out);
        } else if (msg instanceof PubAck) {
            PUB_ACK_MESSAGE_ENCODER.encode(ctx, (PubAck) msg, out);
        } else if (msg instanceof PubRec) {
            PUB_REC_MESSAGE_ENCODER.encode(ctx, (PubRec) msg, out);
        } else if (msg instanceof PubRel) {
            PUB_REL_MESSAGE_ENCODER.encode(ctx, (PubRel) msg, out);
        } else if (msg instanceof PubComp) {
            PUB_COMP_MESSAGE_ENCODER.encode(ctx, (PubComp) msg, out);
        } else if (msg instanceof ConnAck) {
            CONN_ACK_MESSAGE_ENCODER.encode(ctx, (ConnAck) msg, out);
        } else if (msg instanceof SubAck) {
            SUB_ACK_MESSAGE_ENCODER.encode(ctx, (SubAck) msg, out);
        } else if (msg instanceof UnsubAck) {
            UNSUB_ACK_MESSAGE_ENCODER.encode(ctx, (UnsubAck) msg, out);
        } else if (msg instanceof Subscribe) {
            SUBSCRIBE_MESSAGE_ENCODER.encode(ctx, (Subscribe) msg, out);
        } else if (msg instanceof Unsubscribe) {
            UNSUBSCRIBE_MESSAGE_ENCODER.encode(ctx, (Unsubscribe) msg, out);
        } else if (msg instanceof Disconnect) {
            DISCONNECT_MESSAGE_ENCODER.encode(ctx, (Disconnect) msg, out);
        } else if (msg instanceof Connect) {
            CONNECT_MESSAGE_ENCODER.encode(ctx, (Connect) msg, out);
        }
        this.metrics.outgoingMessageSizeMean().update(out.writerIndex());
    }
}
