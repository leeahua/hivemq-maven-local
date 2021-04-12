package bl1;

import bz.LWT;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;

public interface LWTPersistence {
    @Nullable
    LWT get(@NotNull String clientId);

    void persist(@NotNull String clientId, @NotNull LWT lwt);

    @Nullable
    LWT remove(@NotNull String clientId);

    long size();

    boolean isEmpty();

    void clear();

    boolean contains(@NotNull String clientId);
}
