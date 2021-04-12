package u1;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.message.MessageWithId;

public class MessageFlow {
    private final ImmutableList<MessageWithId> messages;

    public MessageFlow(ImmutableList<MessageWithId> messages) {
        this.messages = messages;
    }

    public ImmutableList<MessageWithId> getMessages() {
        return messages;
    }
}
