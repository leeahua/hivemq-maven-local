package al;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.ReturnCode;
import d.CacheScoped;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@CacheScoped
public class ConnectMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectMessageDecoder.class);
    private static final ConnAck REFUSED_UNACCEPTABLE_PROTOCOL_VERSION = new ConnAck(ReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
    private static final ConnectV3_1_1MessageDecoder V_3_1_1_MESSAGE_DECODER = new ConnectV3_1_1MessageDecoder();
    private static final ConnectV3_1MessageDecoder V_3_1_MESSAGE_DECODER = new ConnectV3_1MessageDecoder();

    public Connect decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeard) {
        if (remainingByteBuf.readableBytes() == 0) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with an packet (no protocol version).",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        ByteBuf protocolVersionByteBuf = remainingByteBuf.slice(remainingByteBuf.readerIndex() + 1, 1);
        int protocolVersionByte = protocolVersionByteBuf.readByte();
        ProtocolVersion protocolVersion;
        switch (protocolVersionByte) {
            case 4:
                protocolVersion = ProtocolVersion.MQTTv3_1_1;
                break;
            case 6:
                protocolVersion = ProtocolVersion.MQTTv3_1;
                break;
            default:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) connected with an invalid protocol version.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.writeAndFlush(REFUSED_UNACCEPTABLE_PROTOCOL_VERSION).addListener(ChannelFutureListener.CLOSE);
                return null;
        }
        channel.attr(AttributeKeys.MQTT_VERSION).set(protocolVersion);
        if (protocolVersion == ProtocolVersion.MQTTv3_1_1) {
            return V_3_1_1_MESSAGE_DECODER.decode(channel, remainingByteBuf, fixedHeard);
        }
        if (protocolVersion == ProtocolVersion.MQTTv3_1) {
            return V_3_1_MESSAGE_DECODER.decode(channel, remainingByteBuf, fixedHeard);
        }
        return null;
    }
}
