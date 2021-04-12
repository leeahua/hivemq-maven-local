package a1;

import ab.ClusterResponse;
import com.hivemq.spi.annotations.NotNull;

public interface ClusterReceiver<E> {
    void received(@NotNull E request, @NotNull ClusterResponse response, @NotNull String sender);
}
