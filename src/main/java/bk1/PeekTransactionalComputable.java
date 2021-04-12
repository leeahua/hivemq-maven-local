package bk1;

import bc1.ClientSessionQueueEntry;
import bg1.XodusUtils;
import bj1.QueuedMessagesSerializer;
import com.hivemq.spi.annotations.NotNull;
import jetbrains.exodus.ByteIterable;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Queue;

public class PeekTransactionalComputable
        extends AbstractQueuedMessagesLookupTable
        implements TransactionalComputable<ClientSessionQueueEntry> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PeekTransactionalComputable.class);
    private final String clientId;
    private final Long timestamp;
    private final Store store;
    private final QueuedMessagesSerializer serializer;

    public PeekTransactionalComputable(
            @NotNull String clientId,
            @NotNull Long timestamp,
            @NotNull Map<String, Queue<Long>> lookupTable,
            @NotNull Store store,
            @NotNull QueuedMessagesSerializer serializer) {
        super(lookupTable);
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.store = store;
        this.serializer = serializer;
    }

    @Override
    public ClientSessionQueueEntry compute(@org.jetbrains.annotations.NotNull Transaction txn) {
        ByteIterable searchKey = XodusUtils.toByteIterable(this.clientId);
        Cursor cursor = this.store.openCursor(txn);
        Throwable throwable = null;
        try {
            ByteIterable searchBothRange = cursor.getSearchBothRange(searchKey, XodusUtils.toByteIterable(this.serializer.serializeTimestamp(this.timestamp.longValue())));
            if (searchBothRange != null) {
                do {
                    if (cursor.getKey().compareTo(searchKey) == 0) {
                        return this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue()));
                    }
                } while (cursor.getNext());
            }
            LOGGER.error("Potential inconsistency in the Client Session Queued Message Persistence: No entry for timestamp {} and client {} was found!",
                    this.timestamp, this.clientId);
            return null;
        } catch (Throwable e) {
            throwable = e;
            throw e;
        } finally {
            XodusUtils.close(cursor, throwable);
        }
    }
}
