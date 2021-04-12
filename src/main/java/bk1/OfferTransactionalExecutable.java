package bk1;

import av.PersistenceConfigurationService.QueuedMessagesStrategy;
import bc1.ClientSessionQueueEntry;
import bg1.XodusUtils;
import bj1.QueuedMessagesSerializer;
import bu.InternalPublish;
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

public class OfferTransactionalExecutable implements TransactionalExecutable {
    private static final Logger LOGGER = LoggerFactory.getLogger(OfferTransactionalExecutable.class);
    private final String clientId;
    private final InternalPublish publish;
    private final Map<String, Queue<Long>> lookupTable;
    private final Store store;
    private final QueuedMessagesSerializer serializer;
    private final long maxQueuedMessages;
    @NotNull
    private final QueuedMessagesStrategy strategy;

    public OfferTransactionalExecutable(@NotNull String clientId,
                                        @NotNull InternalPublish publish,
                                        @NotNull Map<String, Queue<Long>> lookupTable,
                                        @NotNull Store store,
                                        @NotNull QueuedMessagesSerializer serializer,
                                        long maxQueuedMessages,
                                        @NotNull QueuedMessagesStrategy strategy) {
        this.clientId = clientId;
        this.publish = publish;
        this.lookupTable = lookupTable;
        this.store = store;
        this.serializer = serializer;
        this.maxQueuedMessages = maxQueuedMessages;
        this.strategy = strategy;
    }

    public void execute(Transaction txn) {
        ByteIterable searchKey = XodusUtils.toByteIterable(this.clientId);
        Queue<Long> queue = this.lookupTable.get(this.clientId);
        if (exists(txn, searchKey, queue)) {
            LOGGER.debug("There is already a entry for client {}, timestamp {} and id {} in the Client Session Queue, ignoring",
                    this.clientId, this.publish.getTimestamp(), this.publish.getUniqueId());
            return;
        }
        while (queue.size() >= this.maxQueuedMessages) {
            if (this.strategy == QueuedMessagesStrategy.DISCARD_OLDEST) {
                deleteOldest(txn, queue.poll());
                LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Deleting oldest entry with timestamp {}",
                        this.maxQueuedMessages, this.clientId, this.publish.getTimestamp());
            } else {
                LOGGER.debug("Maximum Queued Message size of {} for client {} was already reached. Discarding message with timestamp {}",
                        this.maxQueuedMessages, this.clientId, this.publish.getTimestamp());
                return;
            }
        }
        ClientSessionQueueEntry entry = new ClientSessionQueueEntry(this.publish.getSequence(), this.publish.getTimestamp(), this.publish.getTopic(), this.publish.getQoS(), this.publish.getPayload(), this.publish.getClusterId());
        this.store.put(txn, searchKey, XodusUtils.toByteIterable(this.serializer.serialize(entry)));
        queue.offer(this.publish.getTimestamp());
        LOGGER.trace("Added entry for client {} with timestamp {} and id {} to Client Session Queue",
                this.clientId, this.publish.getTimestamp(), this.publish.getUniqueId());
    }

    private void deleteOldest(@NotNull Transaction txn, long timestamp) {
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            ByteIterable searchBothRange = cursor.getSearchBothRange(XodusUtils.toByteIterable(this.clientId), XodusUtils.toByteIterable(this.serializer.serializeTimestamp(timestamp)));
            do {
                if (searchBothRange != null) {
                    ByteIterable clientIdByteIterable = XodusUtils.toByteIterable(this.clientId);
                    if (cursor.getKey().compareTo(clientIdByteIterable) == 0) {
                        do {
                            ClientSessionQueueEntry entry = this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                            if (entry.getTimestamp() == timestamp) {
                                cursor.deleteCurrent();
                            }
                        } while (cursor.getNextDup());
                        return;
                    }
                }
            } while (cursor.getNext());
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            XodusUtils.close(cursor, throwable);
        }
    }

    private boolean exists(Transaction txn, ByteIterable searchKey, Queue<Long> queue) {
        if (!queue.contains(this.publish.getTimestamp())) {
            return false;
        }
        byte[] existsKey = this.serializer.serialize(this.publish.getTimestamp(), this.publish.getClusterId(), this.publish.getSequence());
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            ByteIterable existsKeyByteIterable = XodusUtils.toByteIterable(existsKey);
            ByteIterable foundValues = cursor.getSearchKey(searchKey);
            if (foundValues != null) {
                do {
                    ByteIterable foundValue = cursor.getValue();
                    if (foundValue.subIterable(0, existsKeyByteIterable.getLength()).compareTo(existsKeyByteIterable) == 0) {
                        return true;
                    }
                } while (cursor.getNextDup());
            }
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            XodusUtils.close(cursor, throwable);
        }
        return false;
    }
}
