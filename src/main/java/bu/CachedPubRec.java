package bu;

import com.hivemq.spi.message.PubRec;

public class CachedPubRec extends PubRec {
    public CachedPubRec(int messageId) {
        super(messageId);
    }

    public void setMessageId(int messageId) {
        throw new UnsupportedOperationException("Cached MQTT messages are immutable.");
    }
}
