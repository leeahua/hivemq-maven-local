package b;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.services.configuration.entity.Listener;
import io.netty.channel.ChannelFuture;

public class ListenerStartFuture {
    private final Listener listener;
    private final ChannelFuture future;

    public ListenerStartFuture(@NotNull Listener listener, @NotNull ChannelFuture future) {
        Preconditions.checkNotNull(listener, "Listener must not be null");
        Preconditions.checkNotNull(future, "Future must not be null");
        this.listener = listener;
        this.future = future;
    }

    @NotNull
    public Listener getListener() {
        return listener;
    }

    @NotNull
    public ChannelFuture getFuture() {
        return future;
    }
}
