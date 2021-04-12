package bu;

import com.hivemq.spi.message.PubRel;

public class CachedPubRel extends PubRel {
    public CachedPubRel(int messageId) {
        super(messageId);
    }

    public void setMessageId(int messageId) {
        throw new UnsupportedOperationException("Cached MQTT messages are immutable.");
    }
}
