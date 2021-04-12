package bg1;

import bi.CachedMessages;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.PubRel;
import com.hivemq.spi.message.Publish;

@ThreadSafe
public class IncomingMessageFlowSerializer {
    public static final int MESSAGE_TYPE_PUBLISH = 1;
    public static final int MESSAGE_TYPE_PUB_REL = 2;
    private final CachedMessages cachedMessages = CachedMessages.INSTANCE;

    @ThreadSafe
    public byte[] serialize(MessageWithId messageWithId) {
        byte[] data = new byte[3];
        int messageType;
        if (messageWithId instanceof Publish) {
            messageType = MESSAGE_TYPE_PUBLISH;
        } else if (messageWithId instanceof PubRel) {
            messageType = MESSAGE_TYPE_PUB_REL;
        } else {
            throw new IllegalArgumentException("Only PUBLISHes and PUBRELs can be serialized");
        }
        int messageId = messageWithId.getMessageId();
        data[0] = ((byte) messageType);
        data[1] = ((byte) (messageId >>> 8));
        data[2] = ((byte) messageId);
        return data;
    }

    @ThreadSafe
    public MessageWithId deserialize(byte[] data) {
        if (data.length < 3) {
            throw new IllegalArgumentException("Byte array for deserializing must be at least of size 3 but was " + data.length);
        }
        int messageId = (data[1] & 0xFF) << 8 | data[2] & 0xFF;
        byte messageType = data[0];
        if (messageType == MESSAGE_TYPE_PUBLISH) {
            Publish publish = new Publish();
            publish.setMessageId(messageId);
            return publish;
        }
        if (messageType == MESSAGE_TYPE_PUB_REL) {
            return this.cachedMessages.getPubRel(messageId);
        }
        throw new IllegalArgumentException("Tried to deserialize a message which is not a PubRel or Publish");
    }
}
