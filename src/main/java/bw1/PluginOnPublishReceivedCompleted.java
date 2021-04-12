package bw1;

import bu.InternalPublish;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.security.ClientData;

public class PluginOnPublishReceivedCompleted {
    private final ClientData clientData;
    private final InternalPublish publish;

    public PluginOnPublishReceivedCompleted(@NotNull InternalPublish publish,
                                            @NotNull ClientData clientData) {
        this.clientData = clientData;
        this.publish = Preconditions.checkNotNull(publish, "Publish must not be null");
    }

    @NotNull
    public ClientData getClientData() {
        return clientData;
    }

    @NotNull
    public InternalPublish getPublish() {
        return publish;
    }

    public long getTimestamp() {
        return this.publish.getTimestamp();
    }
}
