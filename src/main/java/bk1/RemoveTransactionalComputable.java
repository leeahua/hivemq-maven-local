package bk1;

import bc1.ClientSessionQueueEntry;
import bg1.XodusUtils;
import bj1.QueuedMessagesSerializer;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalExecutable;

import java.util.Map;
import java.util.Queue;

public class RemoveTransactionalComputable
        extends AbstractQueuedMessagesLookupTable
        implements TransactionalExecutable {
    private final String clientId;
    private final long entryTimestamp;
    private final String entryId;
    private final Store store;
    private final QueuedMessagesSerializer serializer;

    public RemoveTransactionalComputable(@NotNull String clientId,
                                         long entryTimestamp,
                                         @NotNull String entryId,
                                         @NotNull Map<String, Queue<Long>> lookupTable,
                                         @NotNull Store store,
                                         @NotNull QueuedMessagesSerializer serializer) {
        super(lookupTable);
        this.clientId = clientId;
        this.entryTimestamp = entryTimestamp;
        this.entryId = entryId;
        this.store = store;
        this.serializer = serializer;
    }

    public void execute(Transaction txn) {
        doRemove(txn);
    }


    private void doRemove(Transaction txn) {
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            ByteIterable searchBothRange = cursor.getSearchBothRange(XodusUtils.toByteIterable(this.clientId), XodusUtils.toByteIterable(this.serializer.serializeTimestamp(this.entryTimestamp)));
            do {
                if (searchBothRange != null) {
                    ByteIterable clientIdByteIterable = XodusUtils.toByteIterable(this.clientId);
                    if (cursor.getKey().compareTo(clientIdByteIterable) == 0) {
                        do {
                            removeCurrent(cursor);
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

    private void removeCurrent(Cursor cursor) {
        ClientSessionQueueEntry entry = this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
        if (entry.getTimestamp() == this.entryTimestamp &&
                entry.getUniqueId().equals(this.entryId)) {
            cursor.deleteCurrent();
            removeLookupTable(this.clientId, this.entryTimestamp);
        }
    }
}
