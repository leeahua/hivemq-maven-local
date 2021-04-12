package bj;

import com.google.common.collect.ImmutableMap;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.message.ReturnCode;

import java.util.Map;
import java.util.Optional;

public class ConnAcks {
    public static final ConnAck ACCEPTED = new ConnAck(ReturnCode.ACCEPTED, false);
    public static final ConnAck ACCEPTED_AND_SESSION_PRESENT = new ConnAck(ReturnCode.ACCEPTED, true);
    public static final ConnAck REFUSED_UNACCEPTABLE_PROTOCOL_VERSION = new ConnAck(ReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
    public static final ConnAck REFUSED_IDENTIFIER_REJECTED = new ConnAck(ReturnCode.REFUSED_IDENTIFIER_REJECTED);
    public static final ConnAck REFUSED_SERVER_UNAVAILABLE = new ConnAck(ReturnCode.REFUSED_SERVER_UNAVAILABLE);
    public static final ConnAck REFUSED_BAD_USERNAME_OR_PASSWORD = new ConnAck(ReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD);
    public static final ConnAck REFUSED_NOT_AUTHORIZED = new ConnAck(ReturnCode.REFUSED_NOT_AUTHORIZED);
    private static final Map<ReturnCode, ConnAck> REFUSEDS = initRefuseds();
    private static final Map<Boolean, ConnAck> ACCEPTEDS = initAccepteds();

    public static ConnAck accepted(boolean sessionPresent) {
        return ACCEPTEDS.get(sessionPresent);
    }

    public static ConnAck refused(ReturnCode returnCode) {
        return Optional.ofNullable(REFUSEDS.get(returnCode))
                .orElseThrow(() -> new IllegalArgumentException("Unknown return code"));
    }

    private static Map<Boolean, ConnAck> initAccepteds() {
        ImmutableMap.Builder<Boolean, ConnAck> builder = ImmutableMap.builder();
        builder.put(Boolean.TRUE, ACCEPTED_AND_SESSION_PRESENT);
        builder.put(Boolean.FALSE, ACCEPTED);
        return builder.build();
    }

    private static Map<ReturnCode, ConnAck> initRefuseds() {
        ImmutableMap.Builder<ReturnCode, ConnAck> builder = ImmutableMap.builder();
        builder.put(ReturnCode.REFUSED_UNACCEPTABLE_PROTOCOL_VERSION, REFUSED_UNACCEPTABLE_PROTOCOL_VERSION);
        builder.put(ReturnCode.REFUSED_IDENTIFIER_REJECTED, REFUSED_IDENTIFIER_REJECTED);
        builder.put(ReturnCode.REFUSED_SERVER_UNAVAILABLE, REFUSED_SERVER_UNAVAILABLE);
        builder.put(ReturnCode.REFUSED_BAD_USERNAME_OR_PASSWORD, REFUSED_BAD_USERNAME_OR_PASSWORD);
        builder.put(ReturnCode.REFUSED_NOT_AUTHORIZED, REFUSED_NOT_AUTHORIZED);
        return builder.build();
    }
}
