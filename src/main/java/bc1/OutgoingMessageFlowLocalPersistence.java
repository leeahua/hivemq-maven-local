package bc1;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.message.MessageWithId;

import java.util.Set;

public interface OutgoingMessageFlowLocalPersistence {
    @Nullable
    MessageWithId get(@NotNull String clientId, int messageId);

    void addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message);

    @Nullable
    MessageWithId remove(@NotNull String clientId, int messageId);

    void removeAll(@NotNull String clientId);

    int size(@NotNull String clientId);

    ImmutableList<MessageWithId> drain(@NotNull String clientId);

    @ReadOnly
    Set<String> getClients();

    void putAll(@NotNull String clientId, @NotNull ImmutableList<MessageWithId> messages);

    void close();
}
