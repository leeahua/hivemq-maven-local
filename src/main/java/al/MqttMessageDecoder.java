package al;

import am1.Metrics;
import cb1.ChannelUtils;
import com.hivemq.spi.message.MessageType;
import com.hivemq.spi.services.configuration.ThrottlingConfigurationService;
import e.ChannelPipelineDependencies;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MqttMessageDecoder extends ByteToMessageDecoder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MqttMessageDecoder.class);
    public static final int MAX_REMAINING_LENGTH = 2097152;
    public static final int MALFORMED_REMAINING_LENGTH = -1;
    public static final int NEEDED_MORE_DATA_TO_DECODE = -2;
    private final ConnectMessageDecoder connectMessageDecoder;
    private final ConnAckMessageDecoder connAckMessageDecoder;
    private final PingReqMessageDecoder pingReqMessageDecoder;
    private final PublishMessageDecoder publishMessageDecoder;
    private final PubAckMessageDecoder pubAckMessageDecoder;
    private final PubRecMessageDecoder pubRecMessageDecoder;
    private final PubCompMessageDecoder pubCompMessageDecoder;
    private final PubRelMessageDecoder pubRelMessageDecoder;
    private final DisconnectMessageDecoder disconnectMessageDecoder;
    private final SubscribeMessageDecoder subscribeMessageDecoder;
    private final UnsubscribeMessageDecoder unsubscribeMessageDecoder;
    private final SubAckMessageDecoder subAckMessageDecoder;
    private final UnsubAckMessageDecoder unsubAckMessageDecoder;
    private final Metrics metrics;
    private final ThrottlingConfigurationService throttlingConfigurationService;
    private final boolean isServer;

    public MqttMessageDecoder(ConnectMessageDecoder connectMessageDecoder,
                              ConnAckMessageDecoder connAckMessageDecoder,
                              PingReqMessageDecoder pingReqMessageDecoder,
                              PublishMessageDecoder publishMessageDecoder,
                              PubAckMessageDecoder pubAckMessageDecoder,
                              PubRecMessageDecoder pubRecMessageDecoder,
                              PubCompMessageDecoder pubCompMessageDecoder,
                              PubRelMessageDecoder pubRelMessageDecoder,
                              DisconnectMessageDecoder disconnectMessageDecoder,
                              SubscribeMessageDecoder subscribeMessageDecoder,
                              UnsubscribeMessageDecoder unsubscribeMessageDecoder,
                              SubAckMessageDecoder subAckMessageDecoder,
                              UnsubAckMessageDecoder unsubAckMessageDecoder,
                              Metrics metrics,
                              boolean isServer,
                              ThrottlingConfigurationService throttlingConfigurationService) {
        this.connectMessageDecoder = connectMessageDecoder;
        this.connAckMessageDecoder = connAckMessageDecoder;
        this.pingReqMessageDecoder = pingReqMessageDecoder;
        this.publishMessageDecoder = publishMessageDecoder;
        this.pubAckMessageDecoder = pubAckMessageDecoder;
        this.pubRecMessageDecoder = pubRecMessageDecoder;
        this.pubCompMessageDecoder = pubCompMessageDecoder;
        this.pubRelMessageDecoder = pubRelMessageDecoder;
        this.disconnectMessageDecoder = disconnectMessageDecoder;
        this.subscribeMessageDecoder = subscribeMessageDecoder;
        this.unsubscribeMessageDecoder = unsubscribeMessageDecoder;
        this.subAckMessageDecoder = subAckMessageDecoder;
        this.unsubAckMessageDecoder = unsubAckMessageDecoder;
        this.metrics = metrics;
        this.throttlingConfigurationService = throttlingConfigurationService;
        this.isServer = isServer;
    }

    public MqttMessageDecoder(ChannelPipelineDependencies dependencies) {
        this(dependencies.getConnectMessageDecoder(),
                dependencies.getConnAckMessageDecoder(),
                dependencies.getPingReqMessageDecoder(),
                dependencies.getPublishMessageDecoder(),
                dependencies.getPubAckMessageDecoder(),
                dependencies.getPubRecMessageDecoder(),
                dependencies.getPubCompMessageDecoder(),
                dependencies.getPubRelMessageDecoder(),
                dependencies.getDisconnectMessageDecoder(),
                dependencies.getSubscribeMessageDecoder(),
                dependencies.getUnsubscribeMessageDecoder(),
                dependencies.getSubAckMessageDecoder(),
                dependencies.getUnsubAckMessageDecoder(),
                dependencies.getMetrics(),
                true,
                dependencies.getThrottlingConfigurationService());
    }

    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int bufferByteSize = in.readableBytes();
        if (bufferByteSize < 2) {
            return;
        }
        in.markReaderIndex();
        byte fixedHeard = in.readByte();
        int remainingLength = decodeRemainingLength(in);
        if (remainingLength == NEEDED_MORE_DATA_TO_DECODE) {
            in.resetReaderIndex();
            return;
        }
        if (remainingLength == MALFORMED_REMAINING_LENGTH) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected but the remaining length was malformed. Disconnecting client",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            }
            ctx.close();
            in.clear();
            return;
        }
        if (in.readableBytes() < remainingLength) {
            in.resetReaderIndex();
            return;
        }
        int remainingLengthFieldLength = determineRemainingLengthFieldLength(remainingLength);
        if (remainingLength + remainingLengthFieldLength >= this.throttlingConfigurationService.maxMessageSize()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a message, that was bigger than the maximum message size. Disconnecting client",
                        ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
            }
            ctx.close();
            in.clear();
            return;
        }
        this.metrics.incomingMessageSizeMean().update(remainingLength + remainingLengthFieldLength);
        ByteBuf remainingByteBuf = in.readSlice(remainingLength);
        in.markReaderIndex();
        MessageType messageType = determineMessageType(fixedHeard);
        Object msg;
        switch (messageType) {
            case RESERVED_ZERO:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a message with an invalid message type '0'. This message type is reserved. Disconnecting client",
                            ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                }
                ctx.close();
                in.clear();
                return;
            case CONNECT:
                msg = this.connectMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case CONNACK:
                if (this.isServer) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("A client (IP: {}) sent a ConnAck message. This is invalid because clients are not allowed to send CONNACKs. Disconnecting clients",
                                ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                    }
                    ctx.close();
                    in.clear();
                    return;
                }
                msg = this.connAckMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PUBLISH:
                msg = this.publishMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                this.metrics.incomingPublishSizeMean().update(remainingLength + remainingLengthFieldLength);
                break;
            case PUBACK:
                msg = this.pubAckMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PUBREC:
                msg = this.pubRecMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PUBREL:
                msg = this.pubRelMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PUBCOMP:
                msg = this.pubCompMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case SUBSCRIBE:
                msg = this.subscribeMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case SUBACK:
                if (this.isServer) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("A client (IP: {}) sent a SubAck message. This is invalid because clients are not allowed to send SUBACKs. Disconnecting client",
                                ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                    }
                    ctx.close();
                    in.clear();
                    return;
                }
                msg = this.subAckMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case UNSUBSCRIBE:
                msg = this.unsubscribeMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case UNSUBACK:
                if (this.isServer) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("A client (IP: {}) sent a UnsubAck message. This is invalid because clients are not allowed to send UNSUBACKs. Disconnecting client",
                                ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                    }
                    ctx.close();
                    in.clear();
                    return;
                }
                msg = this.unsubAckMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PINGREQ:
                msg = this.pingReqMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case PINGRESP:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a PingResp message. This is invalid because clients are not allowed to send PINGRESPs. Disconnecting client",
                            ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                }
                ctx.close();
                in.clear();
                return;
            case DISCONNECT:
                msg = this.disconnectMessageDecoder.decode(ctx.channel(), remainingByteBuf, fixedHeard);
                break;
            case RESERVED_15:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) connected with an invalid message type '15'. This message type is reserved. Disconnecting client",
                            ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                }
                ctx.close();
                in.clear();
                return;
            default:
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) connected but the message type could not get determined. Disconnecting client",
                            ChannelUtils.remoteIP(ctx.channel()).orElse("UNKNOWN"));
                }
                ctx.close();
                in.clear();
                return;
        }
        if (msg != null) {
            out.add(msg);
        }
    }

    private int determineRemainingLengthFieldLength(int remainingLength) {
        int remainingLengthFieldLength = 2;
        if (remainingLength > 127) {
            remainingLengthFieldLength++;
        }
        if (remainingLength > 16383) {
            remainingLengthFieldLength++;
        }
        if (remainingLength > 2097151) {
            remainingLengthFieldLength++;
        }
        return remainingLengthFieldLength;
    }

    private int decodeRemainingLength(ByteBuf in) {
        int remainingLength = 0;
        int multiplier = 1;
        int digit;
        do {
            if (multiplier > MAX_REMAINING_LENGTH) {
                in.skipBytes(in.readableBytes());
                return MALFORMED_REMAINING_LENGTH;
            }
            if (!in.isReadable()) {
                return NEEDED_MORE_DATA_TO_DECODE;
            }
            digit = in.readByte();
            remainingLength += (digit & 0x7F) * multiplier;
            multiplier *= 128;
        } while ((digit & 0x80) != 0);
        return remainingLength;
    }

    private MessageType determineMessageType(byte messageTypeByte) {
        return MessageType.valueOf((messageTypeByte & 0xF0) >> 4);
    }
}
