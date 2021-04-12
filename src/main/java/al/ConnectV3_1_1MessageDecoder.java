package al;

import cb1.AttributeKeys;
import cb1.ByteBufUtils;
import cb1.ByteUtils;
import cb1.ChannelUtils;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.Connect;
import com.hivemq.spi.message.ProtocolVersion;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.ReturnCode;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectV3_1_1MessageDecoder extends AbstractMessageDecoder<Connect> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectV3_1_1MessageDecoder.class);
    public static final String VERSION = "MQTT";
    private static final int VARIABLE_HEADER_LENGTH = 10;

    public Connect decode(Channel channel, ByteBuf remainingByteBuf, byte fixedHeader) {
        if (!validReservedFlagsBits(fixedHeader)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with an invalid fixed header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            return null;
        }
        if (remainingByteBuf.readableBytes() < VARIABLE_HEADER_LENGTH) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Connect message with an incorrect connect header. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        ByteBuf variableHeader = remainingByteBuf.readSlice(VARIABLE_HEADER_LENGTH);
        if (!validProtocolVersion(variableHeader)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with an invalid protocol version. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            return null;
        }
        int protocolLevel = variableHeader.readByte();
        byte connectFlag = variableHeader.readByte();
        if (!validConnectFlag(connectFlag)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with invalid Connect flags. Disconnecting client",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            return null;
        }
        boolean cleanSession = ByteUtils.getBoolean(connectFlag, 1);
        boolean willFlag = ByteUtils.getBoolean(connectFlag, 2);
        int willQos = (connectFlag & 0x18) >> 3;
        boolean willRetain = ByteUtils.getBoolean(connectFlag, 5);
        boolean hasPassword = ByteUtils.getBoolean(connectFlag, 6);
        boolean hasUserName = ByteUtils.getBoolean(connectFlag, 7);
        if (!validWillTopicFlag(willFlag, willRetain, willQos)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with an invalid willTopic flag combination. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            return null;
        }
        if (!validUsernameAndPassword(hasUserName, hasPassword)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) connected with an invalid username/password combination. The password flag was set but the username flag was not set. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            return null;
        }
        int keepAliveTimeSeconds = variableHeader.readUnsignedShort();
        Connect connect = new Connect();
        connect.setProtocolVersion(ProtocolVersion.MQTTv3_1_1);
        connect.setBridge(false);
        connect.setCleanSession(cleanSession);
        connect.setKeepAliveTimer(keepAliveTimeSeconds);
        connect.setPasswordRequired(hasPassword);
        connect.setUsernameRequired(hasUserName);
        connect.setWill(willFlag);
        connect.setWillRetain(willRetain);
        connect.setWillQos(QoS.valueOf(willQos));
        String clientId = ByteBufUtils.decode(remainingByteBuf);
        if (clientId == null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("A client (IP: {}) sent a Connect message with an incorrect client id length. Disconnecting client.",
                        ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
            }
            channel.close();
            remainingByteBuf.clear();
            return null;
        }
        if (clientId.length() == 0) {
            if (!cleanSession) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) connected with an a persistent session and NO clientID. Using an empty client ID is only allowed when using a cleanSession. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.writeAndFlush(new ConnAck(ReturnCode.REFUSED_IDENTIFIER_REJECTED))
                        .addListener(ChannelFutureListener.CLOSE);
                return null;
            }
            clientId = "gen-" + Integer.toHexString(channel.hashCode()) + "-" + RandomStringUtils.randomAlphanumeric(10);
        }
        channel.attr(AttributeKeys.MQTT_CLIENT_ID).set(clientId);
        channel.attr(AttributeKeys.MQTT_PERSISTENT_SESSION).set(!connect.isCleanSession());
        connect.setClientIdentifier(clientId);
        if (willFlag) {
            String willTopic = ByteBufUtils.decode(remainingByteBuf);
            if (willTopic == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a Connect message with an incorrect will topic length. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            connect.setWillTopic(willTopic);
            byte[] willMessageBytes = ByteUtils.toByteArray(remainingByteBuf);
            connect.setWillMessage(willMessageBytes != null ? willMessageBytes : new byte[0]);
        }
        if (hasUserName) {
            String username = ByteBufUtils.decode(remainingByteBuf);
            if (username == null) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("A client (IP: {}) sent a Connect message with an incorrect username length. Disconnecting client.",
                            ChannelUtils.remoteIP(channel).orElse("UNKNOWN"));
                }
                channel.close();
                remainingByteBuf.clear();
                return null;
            }
            connect.setUsername(username);
            channel.attr(AttributeKeys.AUTH_USERNAME).set(connect.getUsername());
        }
        if (hasPassword) {
            connect.setPassword(ByteUtils.toByteArray(remainingByteBuf));
            channel.attr(AttributeKeys.AUTH_PASSWORD).set(connect.getPassword());
        }
        return connect;
    }

    private boolean validUsernameAndPassword(boolean hasUserName, boolean hasPassword) {
        return !hasPassword || hasUserName;
    }

    private boolean validWillTopicFlag(boolean willFlag, boolean willRetain, int willQos) {
        return willFlag && willQos < 3 ||
                !willRetain && willQos == 0;
    }

    private boolean validConnectFlag(byte connectFlag) {
        return !ByteUtils.getBoolean(connectFlag, 0);
    }

    private boolean validProtocolVersion(ByteBuf variableHeader) {
        return VERSION.equals(ByteBufUtils.decode(variableHeader));
    }
}
