package bn1;

import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.MessageWithId;

public interface IncomingMessageFlowPersistence {
    @Nullable
    MessageWithId get(String clientId, int messageId);

    void addOrReplace(String clientId, int messageId, MessageWithId message);

    void remove(String clientId, int messageId);

    void removeAll(String clientId);
}
