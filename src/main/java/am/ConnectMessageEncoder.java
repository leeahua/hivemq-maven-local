package am;

import cb1.ByteBufUtils;
import cb1.ByteUtils;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;

public class ConnectMessageEncoder extends AbstractMessageEncoder<Connect> {
    private static final byte FIXED_HEADER = 16;
    private static final byte[] MQTT_3_1_PROTOCOL_VERSION = {0, 6, 77, 81, 73, 115, 100, 112, 3};
    private static final byte[] MQTT_3_1_1_PROTOCOL_VERSION = {0, 4, 77, 81, 84, 84, 4};


    protected void encode(ChannelHandlerContext ctx, Connect msg, ByteBuf out) {
        out.writeByte(FIXED_HEADER);
        ByteBuf buffer = Unpooled.buffer();
        if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1) {
            buffer.writeBytes(MQTT_3_1_PROTOCOL_VERSION);
        } else if (msg.getProtocolVersion() == ProtocolVersion.MQTTv3_1_1) {
            buffer.writeBytes(MQTT_3_1_1_PROTOCOL_VERSION);
        } else {
            throw new IllegalArgumentException("Protocol version must be set for a MQTT Connect packet");
        }
        buffer.writeByte(getConnectFlag(msg));
        buffer.writeShort(msg.getKeepAliveTimer());
        buffer.writeBytes(ByteBufUtils.encode(msg.getClientIdentifier()));
        if (msg.isWill()) {
            buffer.writeBytes(ByteBufUtils.encode(msg.getWillTopic()));
            buffer.writeBytes(ByteUtils.toByteBuf(msg.getWillMessage()));
        }
        if (msg.isUsernameRequired()) {
            buffer.writeBytes(ByteBufUtils.encode(msg.getUsername()));
        }
        if (msg.isPasswordRequired()) {
            buffer.writeBytes(ByteUtils.toByteBuf(msg.getPassword()));
        }
        out.writeBytes(encodeRemainingLength(buffer.readableBytes()));
        out.writeBytes(buffer);
    }

    private byte getConnectFlag(Connect connect) {
        byte connectFlag = 0;
        if (connect.isUsernameRequired()) {
            connectFlag = (byte) (connectFlag | 0x80);
        }
        if (connect.isPasswordRequired()) {
            connectFlag = (byte) (connectFlag | 0x40);
        }
        if (connect.isWill()) {
            connectFlag = (byte) (connectFlag | 0x4);
            connectFlag = (byte) (connectFlag | connect.getWillQos().getQosNumber() << 3);
            if (connect.isWillRetain()) {
                connectFlag = (byte) (connectFlag | 0x20);
            }
        }
        if (connect.isCleanSession()) {
            connectFlag = (byte) (connectFlag | 0x2);
        }
        return connectFlag;
    }
}
