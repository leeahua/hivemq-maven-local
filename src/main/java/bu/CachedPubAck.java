package bu;

import com.hivemq.spi.message.PubAck;

public class CachedPubAck extends PubAck {
    public CachedPubAck(int messageId) {
        super(messageId);
    }

    public void setMessageId(int messageId) {
        throw new UnsupportedOperationException("Cached MQTT messages are immutable.");
    }
}
