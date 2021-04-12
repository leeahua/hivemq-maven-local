package bk1;

import bg1.XodusUtils;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveByClientExecutable implements TransactionalExecutable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoveByClientExecutable.class);
    private final String clientId;
    private final Store store;

    public RemoveByClientExecutable(@NotNull String clientId, @NotNull Store store) {
        this.clientId = clientId;
        this.store = store;
    }

    public void execute(Transaction txn) {
        if (this.store.delete(txn, XodusUtils.toByteIterable(this.clientId))) {
            LOGGER.trace("Deleted all queued messages for client {}", this.clientId);
        } else {
            LOGGER.trace("Tried to delete all queued messages for client {}", this.clientId);
        }
    }
}
