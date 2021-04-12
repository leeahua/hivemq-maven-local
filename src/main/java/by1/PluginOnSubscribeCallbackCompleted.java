package by1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.callback.events.OnSubscribeCallback;
import com.hivemq.spi.message.Subscribe;
import com.hivemq.spi.security.ClientData;

import java.util.Queue;

public class PluginOnSubscribeCallbackCompleted {
    private final Queue<OnSubscribeCallback> leftCallbacks;
    private final ClientData clientData;
    private final Subscribe subscribe;
    private final byte[] grantedQoSNumbers;

    public PluginOnSubscribeCallbackCompleted(
            @NotNull Queue<OnSubscribeCallback> leftCallbacks,
            @NotNull ClientData clientData,
            @NotNull Subscribe subscribe,
            byte[] grantedQoSNumbers) {
        this.grantedQoSNumbers = grantedQoSNumbers;
        Preconditions.checkNotNull(leftCallbacks, "Callbacks must not be null");
        Preconditions.checkNotNull(clientData, "Client data must not be null");
        Preconditions.checkNotNull(subscribe, "Subscribe message must not be null");
        this.leftCallbacks = leftCallbacks;
        this.clientData = clientData;
        this.subscribe = subscribe;
    }

    public Queue<OnSubscribeCallback> getLeftCallbacks() {
        return leftCallbacks;
    }

    public ClientData getClientData() {
        return clientData;
    }

    public Subscribe getSubscribe() {
        return subscribe;
    }

    public byte[] getGrantedQoSNumbers() {
        return grantedQoSNumbers;
    }
}
