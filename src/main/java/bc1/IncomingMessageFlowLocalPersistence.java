package bc1;

import bn1.IncomingMessageFlowPersistence;

public interface IncomingMessageFlowLocalPersistence extends IncomingMessageFlowPersistence {
    void close();
}
