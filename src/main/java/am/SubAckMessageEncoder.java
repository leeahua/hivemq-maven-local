package am;

import cb1.AttributeKeys;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.SubAck;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


public class SubAckMessageEncoder extends AbstractMessageEncoder<SubAck> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubAckMessageEncoder.class);
    private static final byte FIXED_HEADER = -112;
    private static final byte REMAINING_LENGTH = 2;

    protected void encode(ChannelHandlerContext ctx, SubAck msg, ByteBuf out) {
        if (valid(ctx, msg)) {
            return;
        }
        out.writeByte(FIXED_HEADER);
        int remainingLength = msg.getGrantedQos().size() + REMAINING_LENGTH;
        out.writeBytes(encodeRemainingLength(remainingLength));
        out.writeShort(msg.getMessageId());
        List<Byte> grantedQos = msg.getGrantedQos();
        grantedQos.forEach(out::writeByte);
    }


    private boolean valid(ChannelHandlerContext ctx, SubAck subAck) {
        ProtocolVersion protocolVersion = ctx.channel().attr(AttributeKeys.MQTT_VERSION).get();
        List<Byte> grantedQos = subAck.getGrantedQos();
        if (grantedQos.size() == 0) {
            LOGGER.error("Tried to write a SubAck with empty payload to a client. This is indicates a HiveMQ bug you should report to support@hivemq.com. Disconnecting client (IP: {}).",
                    ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            ctx.channel().close();
            return true;
        }
        Iterator<Byte> iterator = grantedQos.iterator();
        while (iterator.hasNext()) {
            Byte qoSNumber = iterator.next();
            if (qoSNumber.byteValue() == Byte.MIN_VALUE &&
                    protocolVersion == ProtocolVersion.MQTTv3_1) {
                LOGGER.error("Tried to write a failure code (0x80) to a MQTT 3.1 subscriber. This is indicates a HiveMQ bug you should report to support@hivemq.com. Disconnecting client (IP: {}).",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                ctx.close();
                return true;
            }
            if (qoSNumber.byteValue() != 0 &&
                    qoSNumber.byteValue() != 1 &&
                    qoSNumber.byteValue() != 2 &&
                    qoSNumber.byteValue() != Byte.MIN_VALUE) {
                LOGGER.error("Tried to write an invalid SubAck return code to a subscriber. This is indicates a HiveMQ bug you should report to support@hivemq.com. Disconnecting client (IP: {}).",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                ctx.close();
                return true;
            }
        }
        return false;
    }
}
