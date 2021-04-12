package bg1;

import com.hivemq.spi.annotations.NotNull;

public class OutgoingMessageFlowKey {
    private final String clientId;
    private final int messageId;

    public OutgoingMessageFlowKey(@NotNull String clientId, int messageId) {
        this.clientId = clientId;
        this.messageId = messageId;
    }

    public String getClientId() {
        return clientId;
    }

    public int getMessageId() {
        return messageId;
    }
}