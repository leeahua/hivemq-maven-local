package bg1;

import bc1.OutgoingMessageFlowLocalPersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.message.MessageWithId;
import d.CacheScoped;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class OutgoingMessageFlowLocalMemoryPersistence implements OutgoingMessageFlowLocalPersistence {
    private final Map<String, OutgoingMessageFlowEntity> store = new ConcurrentHashMap<>();

    @ReadOnly
    public Set<String> getClients() {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        return builder.addAll(this.store.keySet()).build();
    }

    public void close() {
        this.store.clear();
    }

    @Nullable
    public MessageWithId get(@NotNull String clientId, int messageId) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        OutgoingMessageFlowEntity entity = this.store.get(clientId);
        if (entity == null) {
            return null;
        }
        return entity.get(messageId);
    }

    public void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        Preconditions.checkNotNull(message, "message must not be null");
        OutgoingMessageFlowEntity entity = this.store.get(clientId);
        if (entity == null) {
            this.store.put(clientId, new OutgoingMessageFlowEntity());
        }
        entity.put(messageId, message);
    }

    @Nullable
    public MessageWithId remove(@NotNull String clientId, int messageId) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        OutgoingMessageFlowEntity entity = this.store.get(clientId);
        if (entity == null) {
            return null;
        }
        return entity.delete(messageId);
    }

    public void removeAll(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client it must not be null");
        this.store.remove(clientId);
    }

    public int size(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client it must not be null");
        OutgoingMessageFlowEntity entity = this.store.get(clientId);
        if (entity == null) {
            return 0;
        }
        return entity.size();
    }

    @ReadOnly
    public ImmutableList<MessageWithId> drain(@NotNull String clientId) {
        Preconditions.checkNotNull(clientId, "client id must not be null");
        OutgoingMessageFlowEntity entity = this.store.get(clientId);
        if (entity == null) {
            return ImmutableList.of();
        }
        List<MessageWithId> messages = new ArrayList(this.store.get(clientId).getMessages());
        Collections.sort(messages, new MessageWithIdComparator());
        return ImmutableList.copyOf(messages);
    }

    public void putAll(@NotNull String clientId, @NotNull ImmutableList<MessageWithId> messages) {
        Preconditions.checkNotNull(clientId, "Client id must not be null");
        Preconditions.checkNotNull(messages, "Messages must not be null");
        OutgoingMessageFlowEntity entity = new OutgoingMessageFlowEntity();
        messages.forEach(message -> entity.put(message.getMessageId(), message));
        this.store.put(clientId, entity);
    }
}
