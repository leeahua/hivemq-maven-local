package bk1;

import av.PersistenceConfigurationService.QueuedMessagesStrategy;
import bc1.ClientSessionQueueEntry;
import bg1.XodusUtils;
import bj1.QueuedMessagesSerializer;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;

public class OfferAllTransactionalComputable
        implements TransactionalExecutable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferAllTransactionalComputable.class);
    private final String clientId;
    private final ImmutableSet<ClientSessionQueueEntry> queueEntries;
    private final Map<String, Queue<Long>> lookupTable;
    private final Store store;
    private final QueuedMessagesSerializer serializer;
    private final long maxQueuedMessages;
    @NotNull
    private final QueuedMessagesStrategy strategy;

    public OfferAllTransactionalComputable(@NotNull String clientId,
                                           @NotNull ImmutableSet<ClientSessionQueueEntry> queueEntries,
                                           @NotNull Map<String, Queue<Long>> lookupTable,
                                           @NotNull Store store,
                                           @NotNull QueuedMessagesSerializer serializer,
                                           long maxQueuedMessages,
                                           @NotNull QueuedMessagesStrategy strategy) {
        this.clientId = clientId;
        this.queueEntries = queueEntries;
        this.lookupTable = lookupTable;
        this.store = store;
        this.serializer = serializer;
        this.maxQueuedMessages = maxQueuedMessages;
        this.strategy = strategy;
    }

    @Override
    public void execute(@org.jetbrains.annotations.NotNull Transaction txn) {
        ByteIterable key = XodusUtils.toByteIterable(this.clientId);
        Queue<Long> queue = this.lookupTable.get(this.clientId);
        queue.clear();
        this.store.delete(txn, key);
        this.queueEntries.forEach(entry -> offer(txn, key, queue, entry));
    }

    private void offer(Transaction txn, ByteIterable key, Queue<Long> queue, ClientSessionQueueEntry entry) {
        while (queue.size() >= this.maxQueuedMessages) {
            if (this.strategy == QueuedMessagesStrategy.DISCARD_OLDEST) {
                removeOldest(txn, queue.poll());
                LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Deleting oldest entry with timestamp {}",
                        this.maxQueuedMessages, this.clientId, entry.getTimestamp());
            } else {
                LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Discarding message with timestamp {}",
                        this.maxQueuedMessages, this.clientId, entry.getTimestamp());
                return;
            }
        }
        this.store.put(txn, key, XodusUtils.toByteIterable(this.serializer.serialize(entry)));
        queue.offer(entry.getTimestamp());
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Added entry for client {} with timestamp {} and id {} to Client Session Queue",
                    this.clientId, entry.getTimestamp(), entry.getUniqueId());
        }
    }

    private void removeOldest(@NotNull Transaction txn, long timestamp) {
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            ByteIterable searchBothRange = cursor.getSearchBothRange(XodusUtils.toByteIterable(this.clientId), XodusUtils.toByteIterable(this.serializer.serializeTimestamp(timestamp)));
            do {
                if (searchBothRange != null) {
                    ByteIterable key = XodusUtils.toByteIterable(this.clientId);
                    if (cursor.getKey().compareTo(key) == 0) {
                        do {
                            ClientSessionQueueEntry queueEntry = this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                            if (queueEntry.getTimestamp() == timestamp) {
                                cursor.deleteCurrent();
                            }
                        } while (cursor.getNextDup());
                        return;
                    }
                }
            } while (cursor.getNext());
        } catch (Throwable localThrowable2) {
            throwable = localThrowable2;
            throw localThrowable2;
        } finally {
            XodusUtils.close(cursor, throwable);
        }
    }


}
