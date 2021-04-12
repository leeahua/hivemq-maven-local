package bz1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.Unsubscribe;
import com.hivemq.spi.security.ClientData;

public class PluginOnUnsubscribe {
    private final ClientData clientData;
    private final Unsubscribe unsubscribe;

    public PluginOnUnsubscribe(@NotNull ClientData clientData,
                               @NotNull Unsubscribe unsubscribe) {
        Preconditions.checkNotNull(clientData, "Client data must not be null");
        Preconditions.checkNotNull(unsubscribe, "Unsubscribe message must not be null");
        this.clientData = clientData;
        this.unsubscribe = unsubscribe;
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }

    @NotNull
    public Unsubscribe getUnsubscribe() {
        return unsubscribe;
    }
}
