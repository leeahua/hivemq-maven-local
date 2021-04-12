package cb1;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import co.PluginExecutorServiceImpl;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExtendedExecutorService implements ListeningExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginExecutorServiceImpl.class);
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private final String name;
    private final String keepAliveSecondsKey;
    private final String minimumPoolSizeKey;
    private final String maximumPoolSize;
    private final String executorName;
    private final String threadNameFormat;
    private ListeningExecutorService executorService;
    private ThreadPoolExecutor executor;

    public ExtendedExecutorService(HiveMQConfigurationService hiveMQConfigurationService,
                                   MetricRegistry metricRegistry,
                                   ShutdownRegistry shutdownRegistry,
                                   String name,
                                   String keepAliveSecondsKey,
                                   String minimumPoolSizeKey,
                                   String maximumPoolSizeKey,
                                   String executorName,
                                   String threadNameFormat) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
        this.name = name;
        this.keepAliveSecondsKey = keepAliveSecondsKey;
        this.minimumPoolSizeKey = minimumPoolSizeKey;
        this.maximumPoolSize = maximumPoolSizeKey;
        this.executorName = executorName;
        this.threadNameFormat = threadNameFormat;
        init();
    }

    public void init() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(this.threadNameFormat).build();
        this.executor = new ThreadPoolExecutor(getConfigMinimumPoolSize(), getConfigurationMaximumPoolSize(), getConfigurationKeepAliveSeconds(), TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(), threadFactory,
                (r, executor) -> {
                    if (executor.isShutdown() ||
                            executor.isTerminated() ||
                            executor.isTerminating()) {
                        LOGGER.trace("Execution for runnable {} was rejected due to shutdown", r.getClass().getCanonicalName());
                    }
                });
        setKeepAliveTime();
        setCorePoolSize();
        setMaximumPoolSize();
        InstrumentedExecutorService delegate = new InstrumentedExecutorService(this.executor, this.metricRegistry, this.executorName);
        this.executorService = MoreExecutors.listeningDecorator(delegate);
        this.shutdownRegistry.register(new ExtendedExecutorServiceShutdown(this));
    }

    private void setKeepAliveTime() {
        int keepAliveSeconds = getConfigurationKeepAliveSeconds();
        this.executor.setKeepAliveTime(keepAliveSeconds, TimeUnit.SECONDS);
        LOGGER.debug("Set executor '{}' thread pool keep-alive to {} seconds", this.name, keepAliveSeconds);
    }

    private void setMaximumPoolSize() {
        int maximumPoolSize = getConfigurationMaximumPoolSize();
        this.executor.setMaximumPoolSize(maximumPoolSize);
        LOGGER.debug("Set executor '{}' max thread pool size to {}", this.name, maximumPoolSize);
    }

    private void setCorePoolSize() {
        int corePoolSize = getConfigMinimumPoolSize();
        this.executor.setCorePoolSize(corePoolSize);
        LOGGER.debug("Set executor '{}' min thread pool size to {}", this.name, corePoolSize);
    }

    private int getConfigurationKeepAliveSeconds() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(this.keepAliveSecondsKey);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", this.keepAliveSecondsKey);
        }
        return 30;
    }

    private int getConfigMinimumPoolSize() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(this.minimumPoolSizeKey);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", this.minimumPoolSizeKey);
        }
        return 0;
    }

    private int getConfigurationMaximumPoolSize() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(this.maximumPoolSize);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", this.maximumPoolSize);
        }
        return 5;
    }

    public int getCorePoolSize() {
        return this.executor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
        return this.executor.getMaximumPoolSize();
    }

    public int getPoolSize() {
        return this.executor.getPoolSize();
    }

    public long getKeepAliveTime() {
        return this.executor.getKeepAliveTime(TimeUnit.SECONDS);
    }

    public String getName() {
        return this.name;
    }

    public void shutdown() {
        this.executorService.shutdown();
    }

    @NotNull
    public List<Runnable> shutdownNow() {
        return this.executorService.shutdownNow();
    }

    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }

    public boolean isTerminated() {
        return this.executorService.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.awaitTermination(timeout, unit);
    }

    @NotNull
    public <T> ListenableFuture<T> submit(Callable<T> task) {
        return this.executorService.submit(task);
    }

    @NotNull
    public <T> ListenableFuture<T> submit(Runnable task, T result) {
        return this.executorService.submit(task, result);
    }

    @NotNull
    public ListenableFuture<?> submit(Runnable task) {
        return this.executorService.submit(task);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executorService.invokeAll(tasks);
    }

    @NotNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.invokeAll(tasks, timeout, unit);
    }

    @NotNull
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return this.executorService.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executorService.invokeAny(tasks, timeout, unit);
    }

    public void execute(Runnable command) {
        this.executorService.execute(command);
    }
}
