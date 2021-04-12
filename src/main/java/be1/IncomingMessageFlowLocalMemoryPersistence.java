package be1;

import bc1.IncomingMessageFlowLocalPersistence;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.MessageWithId;
import d.CacheScoped;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class IncomingMessageFlowLocalMemoryPersistence implements IncomingMessageFlowLocalPersistence {
    private final ConcurrentHashMap<IncomingMessageFlowKey, MessageWithId> store = new ConcurrentHashMap<>();

    @Nullable
    public MessageWithId get(@NotNull String clientId, int messageId) {
        return this.store.get(new IncomingMessageFlowKey(clientId, messageId));
    }

    public void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message) {
        this.store.put(new IncomingMessageFlowKey(clientId, messageId), message);
    }

    @Nullable
    public void remove(@NotNull String clientId, int messageId) {
        this.store.remove(new IncomingMessageFlowKey(clientId, messageId));
    }

    public void removeAll(@NotNull String clientId) {
        Set<IncomingMessageFlowKey> keys = this.store.keySet();
        keys.removeIf(messageKey -> messageKey.getClientId().equals(clientId));
    }

    public void close() {
    }

    private static class IncomingMessageFlowKey {
        private final String clientId;
        private final int messageId;

        public IncomingMessageFlowKey(@NotNull String clientId, int messageId) {
            this.clientId = clientId;
            this.messageId = messageId;
        }

        public String getClientId() {
            return clientId;
        }

        public int getMessageId() {
            return messageId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            IncomingMessageFlowKey that = (IncomingMessageFlowKey) o;
            return messageId == that.messageId &&
                    Objects.equals(clientId, that.clientId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clientId, messageId);
        }
    }
}
