package bn1;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.MessageWithId;

public interface OutgoingMessageFlowSinglePersistence {
    @Nullable
    MessageWithId get(@NotNull String clientId, int messageId);

    ListenableFuture<Void> addOrReplace(@NotNull String clientId, int messageId, @NotNull MessageWithId message);

    @Nullable
    ListenableFuture<MessageWithId> remove(@NotNull String clientId, int messageId);

    ListenableFuture<Void> removeForClient(@NotNull String clientId);

    int size(@NotNull String clientId);

    ImmutableList<MessageWithId> getAll(@NotNull String clientId);
}
