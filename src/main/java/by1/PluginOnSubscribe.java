package by1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

public class PluginOnSubscribe {
    private final Subscribe subscribe;
    private final ClientData clientData;
    private final byte[] grantedQoSNumbers;

    public PluginOnSubscribe(@NotNull Subscribe subscribe,
                             @NotNull ClientData clientData,
                             byte[] grantedQoSNumbers) {
        this.grantedQoSNumbers = grantedQoSNumbers;
        Preconditions.checkNotNull(clientData, "Client data must not be null");
        Preconditions.checkNotNull(subscribe, "Subscribe message must not be null");
        this.subscribe = subscribe;
        this.clientData = clientData;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public byte[] getGrantedQoSNumbers() {
        return grantedQoSNumbers;
    }
}
