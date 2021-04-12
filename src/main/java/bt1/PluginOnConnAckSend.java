package bt1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.ConnAck;
import com.hivemq.spi.security.ClientData;

public class PluginOnConnAckSend {
    @NotNull
    private final ClientData clientData;
    @NotNull
    private final ConnAck connAck;

    public PluginOnConnAckSend(@NotNull ClientData clientData,
                               @NotNull ConnAck connAck) {
        Preconditions.checkNotNull(clientData, "Clientdata must not be null");
        Preconditions.checkNotNull(connAck, "Connack must not be null");
        this.clientData = clientData;
        this.connAck = connAck;
    }

    @NotNull
    public ClientData getClientData() {
        return this.clientData;
    }

    @NotNull
    public ConnAck getConnAck() {
        return this.connAck;
    }
}
