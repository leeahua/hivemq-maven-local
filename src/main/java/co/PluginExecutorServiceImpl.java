package co;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import av.Internals;
import com.codahale.metrics.InstrumentedScheduledExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.spi.services.PluginExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class PluginExecutorServiceImpl implements PluginExecutorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginExecutorServiceImpl.class);
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private final MetricRegistry metricRegistry;
    private final ShutdownRegistry shutdownRegistry;
    private ListeningScheduledExecutorService pluginExecutorService;
    private ScheduledThreadPoolExecutor pluginThreadPoolExecutor;

    @Inject
    public PluginExecutorServiceImpl(HiveMQConfigurationService hiveMQConfigurationService,
                                     MetricRegistry metricRegistry,
                                     ShutdownRegistry shutdownRegistry) {
        this.hiveMQConfigurationService = hiveMQConfigurationService;
        this.metricRegistry = metricRegistry;
        this.shutdownRegistry = shutdownRegistry;
    }

    @PostConstruct
    public void a() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("plugin-executor-%d").build();
        this.pluginThreadPoolExecutor = new ScheduledThreadPoolExecutor(getPluginThreadPoolMinimumSize(), threadFactory);
        setPluginThreadPoolKeepAliveSeconds();
        setPluginThreadPoolMinimumSize();
        setPluginThreadPoolMaximumSize();
        InstrumentedScheduledExecutorService delegate = new InstrumentedScheduledExecutorService(this.pluginThreadPoolExecutor, this.metricRegistry, "com.hivemq.plugin.executor");
        this.pluginExecutorService = MoreExecutors.listeningDecorator(delegate);
        this.shutdownRegistry.register(new PluginExecutorServiceShutdown(this.pluginExecutorService));
    }

    private void setPluginThreadPoolKeepAliveSeconds() {
        int pluginThreadPoolKeepAliveSeconds = getPluginThreadPoolKeepAliveSeconds();
        this.pluginThreadPoolExecutor.setKeepAliveTime(pluginThreadPoolKeepAliveSeconds, TimeUnit.SECONDS);
        LOGGER.debug("Set plugin executor thread pool keep-alive to {} seconds", pluginThreadPoolKeepAliveSeconds);
    }

    private void setPluginThreadPoolMaximumSize() {
        int pluginThreadPoolMaximumSize = getPluginThreadPoolMaximumSize();
        this.pluginThreadPoolExecutor.setMaximumPoolSize(pluginThreadPoolMaximumSize);
        LOGGER.debug("Set plugin executor max thread pool size to {}", pluginThreadPoolMaximumSize);
    }

    private void setPluginThreadPoolMinimumSize() {
        int pluginThreadPoolMinimumSize = getPluginThreadPoolMinimumSize();
        this.pluginThreadPoolExecutor.setCorePoolSize(pluginThreadPoolMinimumSize);
        LOGGER.debug("Set plugin executor min thread pool size to {}", pluginThreadPoolMinimumSize);
    }

    private int getPluginThreadPoolKeepAliveSeconds() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(Internals.PLUGIN_THREAD_POOL_KEEP_ALIVE_SECONDS);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", Internals.PLUGIN_THREAD_POOL_KEEP_ALIVE_SECONDS);
        }
        return 30;
    }

    private int getPluginThreadPoolMinimumSize() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(Internals.PLUGIN_THREAD_POOL_SIZE_MINIMUM);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", Internals.PLUGIN_THREAD_POOL_SIZE_MINIMUM);
        }
        return 0;
    }

    private int getPluginThreadPoolMaximumSize() {
        try {
            String value = this.hiveMQConfigurationService.internalConfiguration().get(Internals.PLUGIN_THREAD_POOL_SIZE_MAXIMUM);
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            LOGGER.debug("Invalid internal configuration setting for {}", Internals.PLUGIN_THREAD_POOL_SIZE_MAXIMUM);
        }
        return 5;
    }

    public int getCorePoolSize() {
        return this.pluginThreadPoolExecutor.getCorePoolSize();
    }

    public int getMaximumPoolSize() {
        return this.pluginThreadPoolExecutor.getMaximumPoolSize();
    }

    public int getPoolSize() {
        return this.pluginThreadPoolExecutor.getPoolSize();
    }

    public long getKeepAliveTime() {
        return this.pluginThreadPoolExecutor.getKeepAliveTime(TimeUnit.SECONDS);
    }

    public ListenableScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return this.pluginExecutorService.schedule(command, delay, unit);
    }

    public <V> ListenableScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return this.pluginExecutorService.schedule(callable, delay, unit);
    }

    public ListenableScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return this.pluginExecutorService.scheduleAtFixedRate(command, initialDelay, period, unit);
    }

    public ListenableScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return this.pluginExecutorService.scheduleWithFixedDelay(command, initialDelay, delay, unit);
    }

    public void shutdown() {
        throw new UnsupportedOperationException("PluginExecutor service can not be shut down manually");
    }

    public void doShutdown() {
        this.pluginExecutorService.shutdown();
    }

    public List<Runnable> shutdownNow() {
        throw new UnsupportedOperationException("PluginExecutor service can not be shut down manually");
    }

    public List<Runnable> doShutdownNow() {
        return this.pluginExecutorService.shutdownNow();
    }

    public boolean isShutdown() {
        return this.pluginExecutorService.isShutdown();
    }

    public boolean isTerminated() {
        return this.pluginExecutorService.isTerminated();
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return this.pluginExecutorService.awaitTermination(timeout, unit);
    }

    public <T> ListenableFuture<T> submit(Callable<T> task) {
        return this.pluginExecutorService.submit(task);
    }

    public <T> ListenableFuture<T> submit(Runnable task, T result) {
        return this.pluginExecutorService.submit(task, result);
    }

    public ListenableFuture<?> submit(Runnable task) {
        return this.pluginExecutorService.submit(task);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return this.pluginExecutorService.invokeAll(tasks);
    }

    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return this.pluginExecutorService.invokeAll(tasks, timeout, unit);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws ExecutionException, InterruptedException {
        return this.pluginExecutorService.invokeAny(tasks);
    }

    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return this.pluginExecutorService.invokeAny(tasks, timeout, unit);
    }

    public void execute(Runnable command) {
        this.pluginExecutorService.execute(command);
    }
}
