package bk1;

import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;

public abstract class AbstractQueuedMessagesLookupTable {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractQueuedMessagesLookupTable.class);
    private final Map<String, Queue<Long>> lookupTable;

    public AbstractQueuedMessagesLookupTable(
            Map<String, Queue<Long>> lookupTable) {
        this.lookupTable = lookupTable;
    }

    protected void removeLookupTable(@NotNull String clientId, long timestamp) {
        Queue<Long> messageQueue = this.lookupTable.get(clientId);
        if (messageQueue == null) {
            return;
        }
        if (messageQueue.remove(timestamp)) {
            LOGGER.trace("Deleted timestamp {} for client {} from ClientSession Queued Messages Lookup Table",
                    timestamp, clientId);
        } else {
            LOGGER.trace("Could not delete entry with timestamp {} for client {} from ClientSession Queued Messages Lookup Table: Not found",
                    timestamp, clientId);
        }
        if (messageQueue.size() == 0) {
            LOGGER.trace("Deleting Client Session Queued Messages Lookup table for client {} because the last queued message was deleted",
                    clientId);
            this.lookupTable.remove(clientId);
        }
    }
}
