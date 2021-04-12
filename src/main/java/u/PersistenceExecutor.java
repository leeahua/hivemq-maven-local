package u;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.spi.metrics.HiveMQMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedTransferQueue;

public class PersistenceExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceExecutor.class);
    private final ExecutorService executorService;
    private final Timer timer;
    private boolean running = false;
    private final BlockingQueue<TaskFuture> taskQueue = new LinkedTransferQueue<>();

    public PersistenceExecutor(String name, MetricRegistry metricRegistry) {
        registerMetric(name, metricRegistry);
        this.timer = metricRegistry.timer(HiveMQMetrics.SINGLE_WRITER_PREFIX + "." + name + ".time");
        this.executorService = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat(name).build());
        this.executorService.submit(()->{
            while (running){
                try {
                    TaskFuture taskFuture = taskQueue.take();
                    Timer.Context timeContext = this.timer.time();
                    try {
                        taskFuture.getSettableFuture().set(taskFuture.getTask().call());
                    } catch (Throwable e) {
                        taskFuture.getSettableFuture().setException(e);
                    } finally {
                        timeContext.stop();
                    }
                } catch (InterruptedException e) {
                    LOGGER.error("Single writer service {} was interrupted. ", name, e);
                }
            }
        });
    }

    private void registerMetric(String name, MetricRegistry metricRegistry) {
        metricRegistry.register(HiveMQMetrics.SINGLE_WRITER_PREFIX + "." + name + ".queue-size",
                new Gauge<Integer>() {
                    @Override
                    public Integer getValue() {
                        return taskQueue.size();
                    }
                });
    }

    public <R> ListenableFuture<R> add(PersistTask<R> task) {
        SettableFuture settableFuture = SettableFuture.create();
        this.taskQueue.add(new TaskFuture(settableFuture, task));
        return settableFuture;
    }


    public void run() {
        this.running = true;
    }

    @FunctionalInterface
    public interface PersistTask<R> {
        R call();
    }

    private class TaskFuture {
        private final SettableFuture settableFuture;
        private final PersistTask task;

        private TaskFuture(SettableFuture settableFuture, PersistTask task) {
            this.settableFuture = settableFuture;
            this.task = task;
        }

        public SettableFuture getSettableFuture() {
            return settableFuture;
        }

        public PersistTask getTask() {
            return task;
        }
    }
}
