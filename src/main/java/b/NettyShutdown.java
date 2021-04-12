package b;

import ap.Shutdown;
import io.netty.channel.EventLoopGroup;
import io.netty.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class NettyShutdown extends Shutdown {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyShutdown.class);
    private final EventLoopGroup childEventLoop;
    private final EventLoopGroup parentEventLoop;
    private final int timeout;

    public NettyShutdown(EventLoopGroup childEventLoop,
                         EventLoopGroup parentEventLoop,
                         int timeout) {
        this.childEventLoop = childEventLoop;
        this.parentEventLoop = parentEventLoop;
        this.timeout = timeout;
    }

    public String name() {
        return "Netty Shutdown";
    }

    public Priority priority() {
        return Priority.MEDIUM;
    }

    public boolean isAsync() {
        return false;
    }

    public void run() {
        LOGGER.debug("Shutting down worker and boss threads");
        Future<?> workerShutdownFuture = this.childEventLoop.shutdownGracefully(2L, this.timeout, TimeUnit.SECONDS);
        Future<?> bossShutdownFuture = this.parentEventLoop.shutdownGracefully(2L, this.timeout, TimeUnit.SECONDS);
        LOGGER.trace("Waiting for Worker threads to finish");
        workerShutdownFuture.syncUninterruptibly();
        LOGGER.trace("Waiting for Boss threads to finish");
        bossShutdownFuture.syncUninterruptibly();
    }
}
