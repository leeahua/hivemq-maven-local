package bu;

import com.hivemq.spi.message.PubComp;

public class CachedPubComp extends PubComp {
    public CachedPubComp(int messageId) {
        super(messageId);
    }

    public void setMessageId(int messageId) {
        throw new UnsupportedOperationException("Cached MQTT messages are immutable.");
    }
}
