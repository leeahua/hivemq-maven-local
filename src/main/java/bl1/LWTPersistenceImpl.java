package bl1;

import bc1.LWTLocalPersistence;
import bz.LWT;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;

import javax.inject.Inject;

public class LWTPersistenceImpl implements LWTPersistence {
    private final LWTLocalPersistence localPersistence;

    @Inject
    public LWTPersistenceImpl(LWTLocalPersistence localPersistence) {
        this.localPersistence = localPersistence;
    }

    @Nullable
    public LWT get(@NotNull String clientId) {
        return this.localPersistence.get(clientId);
    }

    public void persist(@NotNull String clientId, @NotNull LWT lwt) {
        this.localPersistence.persist(clientId, lwt);
    }

    @Nullable
    public LWT remove(@NotNull String clientId) {
        return this.localPersistence.remove(clientId);
    }

    public long size() {
        return this.localPersistence.size();
    }

    public boolean isEmpty() {
        return size() == 0L;
    }

    public void clear() {
        this.localPersistence.clear();
    }

    public boolean contains(@NotNull String clientId) {
        return this.localPersistence.contains(clientId);
    }
}
