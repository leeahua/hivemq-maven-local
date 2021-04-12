package cb1;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import com.codahale.metrics.InstrumentedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
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
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ExtendedScheduledExecutorService implements ListeningScheduledExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedScheduledExecutorService.class);
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private final String executorName;
    private final String minimumPoolSizeKey;
    private final String name;
    private final String threadNameFormat;
    private ListeningScheduledExecutorService executorService;
    private ScheduledThreadPoolExecutor executor;

    public ExtendedScheduledExecutorService(HiveMQConfigurationService hiveMQConfigurationService,
                                            MetricRegistry metricRegistry,
                                            ShutdownRegistry shutdownRegistry,
                                            String executorName,
                                            String minimumPoolSizeKey,
                                            String name,
                                            String threadNameFormat) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
        this.executorName = executorName;
        this.minimumPoolSizeKey = minimumPoolSizeKey;
        this.name = name;
        this.threadNameFormat = threadNameFormat;
        init();
    }

    public void init() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(this.threadNameFormat).build();
        this.executor = new ScheduledThreadPoolExecutor(getConfigMinimumPoolSize(), threadFactory);
        setCorePoolSize();
        InstrumentedScheduledExecutorService delegate =
                new InstrumentedScheduledExecutorService(this.executor, this.metricRegistry, this.name);
        this.executorService = MoreExecutors.listeningDecorator(delegate);
        this.shutdownRegistry.register(new ExtendedScheduledExecutorServiceShutdown(this));
    }

    private void setCorePoolSize() {
        int minimumPoolSize = getConfigMinimumPoolSize();
        this.executor.setCorePoolSize(minimumPoolSize);
        LOGGER.debug("Set executor '{}' min thread pool size to {}", this.executorName, minimumPoolSize);
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

    public String getExecutorName() {
        return this.executorName;
    }

    @Override
    public void shutdown() {
        this.executorService.shutdown();
    }

    @Override
    @NotNull
    public List<Runnable> shutdownNow() {
        return this.executorService.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return this.executorService.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return this.executorService.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.awaitTermination(timeout, unit);
    }

    @Override
    @NotNull
    public <T> ListenableFuture<T> submit(Callable<T> task) {
        return this.executorService.submit(task);
    }

    @Override
    @NotNull
    public <T> ListenableFuture<T> submit(Runnable task, T result) {
        return this.executorService.submit(task, result);
    }

    @Override
    @NotNull
    public ListenableFuture<?> submit(Runnable task) {
        return this.executorService.submit(task);
    }

    @Override
    @NotNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.executorService.invokeAll(tasks);
    }

    @Override
    @NotNull
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.executorService.invokeAll(tasks, timeout, unit);
    }

    @Override
    @NotNull
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return this.executorService.invokeAny(tasks);
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.executorService.invokeAny(tasks, timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        this.executorService.execute(command);
    }

    @Override
    public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.executorService.schedule(command, delay, unit);
    }

    @Override
    public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return this.executorService.schedule(callable, delay, unit);
    }

    @Override
    public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return this.executorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    @Override
    public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return this.executorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }
}
