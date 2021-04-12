package ah;

import com.hivemq.spi.annotations.NotNull;

public interface ClusterStateChangedListener {
    void onChanged(@NotNull ClusterState state, @NotNull String node);
}
