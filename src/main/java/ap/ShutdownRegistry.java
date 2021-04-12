package ap;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MarkerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

@Singleton
public class ShutdownRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownRegistry.class);
    public static final String DEFAULT_SHUTDOWN_NAME = "HiveMQ Shutdown";
    private final Multimap<Integer, Shutdown> blockingShutdowns;
    private final CopyOnWriteArraySet<Shutdown> asyncShutdowns;
    private Thread blockingThread;

    ShutdownRegistry() {
        ListMultimap<Integer, Shutdown> threadListMultimap =
                MultimapBuilder.SortedSetMultimapBuilder
                        .treeKeys(Ordering.natural().reverse())
                        .arrayListValues()
                        .build();
        this.blockingShutdowns = Multimaps.synchronizedListMultimap(threadListMultimap);
        this.asyncShutdowns = new CopyOnWriteArraySet<>();
    }

    @PostConstruct
    public void init() {
        LOGGER.debug("Registering synchronous shutdown hook");
        this.blockingThread = new Thread(() -> {
            LOGGER.info("Shutting down HiveMQ. Please wait, this could take a while...");
            LOGGER.debug("Running synchronous shutdown hook");
            getBlockingShutdowns().values()
                    .forEach(shutdownHook -> {
                        LOGGER.trace(MarkerFactory.getMarker("SHUTDOWN_HOOK"), "Running shutdown hook {}", shutdownHook.name());
                        shutdownHook.run();
                    });
        }, DEFAULT_SHUTDOWN_NAME);
        Runtime.getRuntime().addShutdownHook(this.blockingThread);
    }

    public void register(@NotNull Shutdown shutdownHook) {
        Preconditions.checkNotNull(shutdownHook, "A shutdown hook must not be null");
        if (shutdownHook.isAsync()) {
            LOGGER.debug("Registering asynchronous shutdown hook {} ", shutdownHook.name());
            shutdownHook.setName(shutdownHook.name());
            this.asyncShutdowns.add(shutdownHook);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            return;
        }
        shutdownHook.setName(shutdownHook.name());
        LOGGER.debug("Adding synchronous shutdown hook {} with priority {}",
                shutdownHook.name(), shutdownHook.priority());
        this.blockingShutdowns.put(shutdownHook.priority().getValue(), shutdownHook);
    }

    public Multimap<Integer, Shutdown> getBlockingShutdowns() {
        return this.blockingShutdowns;
    }


    @ReadOnly
    public Set<Shutdown> getAsyncShutdowns() {
        return Collections.unmodifiableSet(this.asyncShutdowns);
    }

    @VisibleForTesting
    Thread getBlockingThread() {
        return this.blockingThread;
    }
}
