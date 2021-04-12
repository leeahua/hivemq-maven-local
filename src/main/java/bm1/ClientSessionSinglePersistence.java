package bm1;

import com.google.common.util.concurrent.ListenableFuture;
import com.hivemq.spi.annotations.NotNull;
import u1.MessageFlow;

public interface ClientSessionSinglePersistence {

    ListenableFuture<Boolean> isPersistent(@NotNull String clientId);

    ListenableFuture<Void> disconnected(String clientId);

    ListenableFuture<MessageFlow> persistent(String clientId, boolean persistentSession);

}
