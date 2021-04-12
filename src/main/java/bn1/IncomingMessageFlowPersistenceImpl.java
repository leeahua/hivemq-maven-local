package bn1;

import bc1.IncomingMessageFlowLocalPersistence;
import com.hivemq.spi.message.MessageWithId;
import d.CacheScoped;

import javax.inject.Inject;

@CacheScoped
public class IncomingMessageFlowPersistenceImpl
        implements IncomingMessageFlowPersistence {
    private final IncomingMessageFlowLocalPersistence localPersistence;

    @Inject
    IncomingMessageFlowPersistenceImpl(IncomingMessageFlowLocalPersistence localPersistence) {
        this.localPersistence = localPersistence;
    }

    public MessageWithId get(String clientId, int messageId) {
        return this.localPersistence.get(clientId, messageId);
    }

    public void addOrReplace(String clientId, int messageId, MessageWithId message) {
        this.localPersistence.addOrReplace(clientId, messageId, message);
    }

    public void remove(String clientId, int messageId) {
        this.localPersistence.remove(clientId, messageId);
    }

    public void removeAll(String clientId) {
        this.localPersistence.removeAll(clientId);
    }
}
