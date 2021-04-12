package bs1;

import com.hivemq.spi.message.QoS;
import com.hivemq.spi.security.ClientData;

public class PluginOnInsufficientPermissionDisconnect {
    private final ClientData clientData;
    private final String topic;
    private final QoS qoS;
    private final Type type;

    public PluginOnInsufficientPermissionDisconnect(
            Type type,
            ClientData clientData,
            String topic,
            QoS qoS) {
        this.clientData = clientData;
        this.topic = topic;
        this.qoS = qoS;
        this.type = type;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public String getTopic() {
        return topic;
    }

    public QoS getQoS() {
        return qoS;
    }

    public Type getType() {
        return type;
    }

    public enum Type {
        SUBSCRIBE,
        PUBLISH
    }
}
