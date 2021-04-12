package by1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

public class PluginOnSubscribeCompleted {
    private final Subscribe subscribe;
    private final byte[] grantedQoSNumbers;
    private final ClientData clientData;

    public PluginOnSubscribeCompleted(@NotNull Subscribe subscribe,
                                      byte[] grantedQoSNumbers,
                                      ClientData clientData) {
        this.grantedQoSNumbers = grantedQoSNumbers;
        this.clientData = clientData;
        Preconditions.checkNotNull(subscribe, "Subscribe message must not be null");
        this.subscribe = subscribe;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public byte[] getGrantedQoSNumbers() {
        return grantedQoSNumbers;
    }

    public ClientData getClientData() {
        return clientData;
    }
}
