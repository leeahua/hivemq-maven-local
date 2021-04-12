package bz;

import ap.Shutdown;
import bc1.ClientSessionLocalPersistence;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import bc1.IncomingMessageFlowLocalPersistence;
import bc1.OutgoingMessageFlowLocalPersistence;
import bc1.QueuedMessagesLocalPersistence;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesLocalPersistence;

public class PersistenceShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceShutdown.class);
    private final ClientSessionLocalPersistence clientSessionLocalPersistence;
    private final ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence;
    private final QueuedMessagesLocalPersistence queuedMessagesLocalPersistence;
    private final OutgoingMessageFlowLocalPersistence outgoingMessageFlowLocalPersistence;
    private final IncomingMessageFlowLocalPersistence incomingMessageFlowLocalPersistence;
    private final RetainedMessagesLocalPersistence retainedMessagesLocalPersistence;

    @Inject
    PersistenceShutdown(ClientSessionLocalPersistence clientSessionLocalPersistence,
                        ClientSessionSubscriptionsLocalPersistence clientSessionSubscriptionsLocalPersistence,
                        QueuedMessagesLocalPersistence queuedMessagesLocalPersistence,
                        OutgoingMessageFlowLocalPersistence outgoingMessageFlowLocalPersistence,
                        IncomingMessageFlowLocalPersistence incomingMessageFlowLocalPersistence,
                        RetainedMessagesLocalPersistence retainedMessagesLocalPersistence) {
        this.clientSessionLocalPersistence = clientSessionLocalPersistence;
        this.clientSessionSubscriptionsLocalPersistence = clientSessionSubscriptionsLocalPersistence;
        this.queuedMessagesLocalPersistence = queuedMessagesLocalPersistence;
        this.outgoingMessageFlowLocalPersistence = outgoingMessageFlowLocalPersistence;
        this.incomingMessageFlowLocalPersistence = incomingMessageFlowLocalPersistence;
        this.retainedMessagesLocalPersistence = retainedMessagesLocalPersistence;
    }

    @NotNull
    public String name() {
        return "Persistence Shutdown";
    }

    @NotNull
    public Priority priority() {
        return Priority.VERY_LOW;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        LOGGER.trace("Closing persistent stores");
        this.clientSessionLocalPersistence.close();
        this.clientSessionSubscriptionsLocalPersistence.close();
        this.queuedMessagesLocalPersistence.close();
        this.outgoingMessageFlowLocalPersistence.close();
        this.incomingMessageFlowLocalPersistence.close();
        this.retainedMessagesLocalPersistence.close();
    }
}
