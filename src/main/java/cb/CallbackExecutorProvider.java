package cb;

import av.HiveMQConfigurationService;
import av.Internals;
import ca.CallbackExecutor;
import ca.CallbackThreadPoolExecutor;
import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.metrics.HiveMQMetrics;
import d.CacheScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CallbackExecutorProvider implements Provider<CallbackExecutor> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackExecutorProvider.class);
    private final MetricRegistry metricRegistry;
    private final HiveMQConfigurationService hiveMQConfigurationService;

    @Inject
    public CallbackExecutorProvider(MetricRegistry metricRegistry,
                                    HiveMQConfigurationService hiveMQConfigurationService) {
        this.metricRegistry = metricRegistry;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
    }

    @CacheScoped
    @Override
    public CallbackExecutor get() {
        ThreadPoolExecutor threadPoolExecutor = createThreadPoolExecutor();
        threadPoolExecutor.allowCoreThreadTimeOut(true);
        InstrumentedExecutorService delegate = new InstrumentedExecutorService(threadPoolExecutor, this.metricRegistry, HiveMQMetrics.CALLBACK_EXECUTOR_PREFIX);
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(delegate);
        MoreExecutors.addDelayedShutdownHook(threadPoolExecutor, 5L, TimeUnit.SECONDS);
        return new CallbackExecutor(executorService);
    }

    @NotNull
    private ThreadPoolExecutor createThreadPoolExecutor() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("hivemq-callback-executor-%d")
                .build();
        int callbackThreadPoolSizeMaximum = getCallbackThreadPoolSizeMaximum();
        LOGGER.debug("Set callback executor thread pool size to {} ", callbackThreadPoolSizeMaximum);
        return new CallbackThreadPoolExecutor(callbackThreadPoolSizeMaximum,
                callbackThreadPoolSizeMaximum,
                getCallbackThreadPoolKeepAliveSeconds(),
                TimeUnit.SECONDS,
                Queues.newLinkedBlockingQueue(),
                threadFactory);
    }

    private int getCallbackThreadPoolKeepAliveSeconds() {
        return this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.CALLBACK_THREAD_POOL_KEEP_ALIVE_SECONDS);
    }

    private int getCallbackThreadPoolSizeMaximum() {
        return this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.CALLBACK_THREAD_POOL_SIZE_MAXIMUM);
    }
}
