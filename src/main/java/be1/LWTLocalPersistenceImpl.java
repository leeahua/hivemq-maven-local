package be1;

import bc1.LWTLocalPersistence;
import bz.LWT;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class LWTLocalPersistenceImpl implements LWTLocalPersistence {
    private static final Logger LOGGER = LoggerFactory.getLogger(LWTLocalPersistenceImpl.class);
    private final ConcurrentHashMap<String, LWT> store = new ConcurrentHashMap<>();

    @Nullable
    public LWT get(@NotNull String clientId) {
        Preconditions.checkNotNull("Client must not be null", clientId);
        LWT lwt = this.store.get(clientId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting LWT message for client {}: {}", clientId, lwt);
        }
        return lwt;
    }

    public void persist(@NotNull String clientId, @NotNull LWT lwt) {
        Preconditions.checkNotNull("Client must not be null", clientId);
        Preconditions.checkNotNull("LWT message must not be null", lwt);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Persisting LWT message for client {}: {}", clientId, lwt);
        }
        this.store.put(clientId, lwt);
    }

    @Nullable
    public LWT remove(@NotNull String clientId) {
        Preconditions.checkNotNull("Client must not be null", clientId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Removing LWT message for client {}", clientId);
        }
        return this.store.remove(clientId);
    }

    public long size() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting LWT store size: {}", this.store.size());
        }
        return this.store.size();
    }

    public boolean isEmpty() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Getting if LWT store is empty: {}", this.store.size());
        }
        return this.store.isEmpty();
    }

    public void clear() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Clearing LWT store");
        }
        this.store.clear();
    }

    public boolean contains(@NotNull String clientId) {
        Preconditions.checkNotNull("Client must not be null", clientId);
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Checking if LWT store contains entry for client {}: {}",
                    clientId, this.store.containsKey(clientId));
        }
        return this.store.containsKey(clientId);
    }
}
