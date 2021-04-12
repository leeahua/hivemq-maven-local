package bg1;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.MessageWithId;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OutgoingMessageFlowEntity {
    private final Map<Integer, MessageWithId> messages = new ConcurrentHashMap<>();

    public void put(int messageId, MessageWithId message) {
        this.messages.put(messageId, message);
    }

    @Nullable
    public MessageWithId get(int messageId) {
        return this.messages.get(messageId);
    }

    public MessageWithId delete(int messageId) {
        return this.messages.remove(messageId);
    }

    public int size() {
        return this.messages.size();
    }

    public List<MessageWithId> getMessages() {
        ImmutableList.Builder<MessageWithId> builder = ImmutableList.builder();
        return builder.addAll(this.messages.values()).build();
    }
}
