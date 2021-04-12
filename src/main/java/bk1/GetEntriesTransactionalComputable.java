package bk1;

import bc1.ClientSessionQueueEntry;
import bg1.XodusUtils;
import bj1.QueuedMessagesSerializer;
import com.google.common.collect.ImmutableMap;
import jetbrains.exodus.env.Cursor;
import jetbrains.exodus.env.Store;
import jetbrains.exodus.env.Transaction;
import jetbrains.exodus.env.TransactionalComputable;
import u.Filter;
import u.TimestampObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GetEntriesTransactionalComputable
        implements TransactionalComputable<ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>>> {
    private final Filter filter;
    private final Store store;
    private final ConcurrentHashMap<String, Long> clientLastTimestamps;
    private final QueuedMessagesSerializer serializer;

    public GetEntriesTransactionalComputable(Filter filter,
                                             Store store,
                                             ConcurrentHashMap<String, Long> clientLastTimestamps,
                                             QueuedMessagesSerializer serializer) {
        this.filter = filter;
        this.store = store;
        this.clientLastTimestamps = clientLastTimestamps;
        this.serializer = serializer;
    }

    @Override
    public ImmutableMap<String, TimestampObject<Set<ClientSessionQueueEntry>>> compute(@org.jetbrains.annotations.NotNull Transaction txn) {
        Map<String, TimestampObject<Set<ClientSessionQueueEntry>>> entries = new HashMap<>();
        Cursor cursor = this.store.openCursor(txn);
        while (cursor.getNext()) {
            String clientId = XodusUtils.toString(cursor.getKey());
            if (!this.filter.test(clientId)) {
                continue;
            }
            TimestampObject<Set<ClientSessionQueueEntry>> timestampObject = entries.get(clientId);
            Set<ClientSessionQueueEntry> clientEntries;
            if (timestampObject == null) {
                clientEntries = new HashSet<>();
                entries.put(clientId, new TimestampObject<>(clientEntries, (this.clientLastTimestamps.get(clientId)).longValue()));
            } else {
                clientEntries = timestampObject.getObject();
            }
            clientEntries.add(this.serializer.deserialize(XodusUtils.toBytes(cursor.getValue())));
        }
        this.clientLastTimestamps.entrySet().stream()
                .filter(entry -> !entries.containsKey(entry.getKey()))
                .forEach(entry ->
                        entries.put(entry.getKey(), new TimestampObject<>(Collections.emptySet(), entry.getValue())));
        return ImmutableMap.copyOf(entries);
    }
}
