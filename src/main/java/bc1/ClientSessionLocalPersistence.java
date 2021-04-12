package bc1;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import u.TimestampObject;
import u.Filter;

import java.util.Map;
import java.util.Set;
import v.ClientSession;

public interface ClientSessionLocalPersistence extends CloseablePersistence {
    ClientSession get(@NotNull String clientId);

    void persistent(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp);

    void remove(@NotNull String clientId);

    ClientSession disconnect(@NotNull String clientId, @NotNull String connectedNode, long timestamp);

    Map<String, TimestampObject<ClientSession>> get(@NotNull Filter filter, int bucketIndex);

    ImmutableSet<String> remove(@NotNull Filter filter, int bucketIndex);

    void merge(@NotNull String clientId, @NotNull ClientSession clientSession, long timestamp);

    ImmutableMap<String, ClientSession> disconnectAll(@NotNull String node, int bucketIndex);

    Set<String> cleanUp(long tombstoneMaxAge, int bucketIndex);

    int persistentSessionSize();
}
