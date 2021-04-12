package am;

import cb1.AttributeKeys;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.ReturnCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.MessageToByteEncoder;

public class ConnAckMessageEncoder extends MessageToByteEncoder<ConnAck> {
    private static final byte FIXED_HEADER = 32;
    private static final byte REMAINING_LENGTH = 2;
    private static final byte CONNECT_CONFIRMATION_FLAG_0 = 0;
    private static final byte CONNECT_CONFIRMATION_FLAG_1 = 1;


    protected void encode(ChannelHandlerContext ctx, ConnAck msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        out.writeByte(REMAINING_LENGTH);
        ReturnCode returnCode = msg.getReturnCode();
        ProtocolVersion protocolVersion = ctx.channel().attr(AttributeKeys.MQTT_VERSION).get();
        switch (protocolVersion) {
            case MQTTv3_1:
                out.writeByte(CONNECT_CONFIRMATION_FLAG_0);
                break;
            case MQTTv3_1_1:
                if (returnCode == ReturnCode.ACCEPTED && msg.isSessionPresent()) {
                    out.writeByte(CONNECT_CONFIRMATION_FLAG_1);
                } else {
                    out.writeByte(CONNECT_CONFIRMATION_FLAG_0);
                }
                break;
        }
        out.writeByte(returnCode.getCode());
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        super.write(ctx, msg, promise);
        if (msg instanceof ConnAck && ((ConnAck) msg).getReturnCode() != ReturnCode.ACCEPTED) {
            promise.addListener(ChannelFutureListener.CLOSE);
        }
    }
}
