package bx1;

import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.ClientData;

public class PluginOnPublishSend {
    private final ClientData clientData;
    private final InternalPublish publish;

    public PluginOnPublishSend(@NotNull ClientData clientData, @NotNull InternalPublish publish) {
        Preconditions.checkNotNull(clientData, "ClientData must not be null");
        Preconditions.checkNotNull(publish, "Message must not be null");
        this.clientData = clientData;
        this.publish = publish;
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }

    @NotNull
    public InternalPublish getPublish() {
        return publish;
    }
}
