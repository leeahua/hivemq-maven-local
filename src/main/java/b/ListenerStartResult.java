package b;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.services.configuration.entity.Listener;

import java.util.Optional;

public class ListenerStartResult {
    private final int port;
    private final boolean hasException;
    private final Listener listener;
    private final Optional<Throwable> exception;

    private ListenerStartResult(int port,
                                boolean hasException,
                                @NotNull Listener listener,
                                @Nullable Throwable exception) {
        Preconditions.checkNotNull(listener, "Original Listener must not be null");
        this.port = port;
        this.hasException = hasException;
        this.listener = listener;
        this.exception = Optional.ofNullable(exception);
    }

    public int getPort() {
        return port;
    }

    public boolean isHasException() {
        return hasException;
    }

    public Listener getListener() {
        return listener;
    }

    public Optional<Throwable> getException() {
        return exception;
    }

    public static ListenerStartResult of(int port, @NotNull Listener listener) {
        return new ListenerStartResult(port, true, listener, null);
    }

    public static ListenerStartResult of(int port, @NotNull Listener listener, @Nullable Throwable paramThrowable) {
        return new ListenerStartResult(port, false, listener, paramThrowable);
    }
}
