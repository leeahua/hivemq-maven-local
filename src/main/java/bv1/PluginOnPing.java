package bv1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.ClientData;

public class PluginOnPing {
    private final ClientData clientData;

    public PluginOnPing(@NotNull ClientData clientData) {
        Preconditions.checkNotNull(clientData, "ClientData must not be null");
        this.clientData = clientData;
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }
}
