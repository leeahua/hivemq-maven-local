package ao1;

import bc1.ClientSessionLocalPersistence;
import com.codahale.metrics.Gauge;

public class ClientSessionsCurrent implements Gauge<Integer> {
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;

    public ClientSessionsCurrent(ClientSessionLocalPersistence clientSessionLocalPersistence) {
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
    }

    @Override
    public Integer getValue() {
        return this.clientSessionLocalPersistence.persistentSessionSize();
    }
}
