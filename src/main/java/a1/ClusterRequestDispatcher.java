package a1;

import ab.ClusterResponse;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterRequest;

public interface ClusterRequestDispatcher {
    void dispatch(@NotNull ClusterRequest request, @NotNull ClusterResponse response, @NotNull String sender);
}
