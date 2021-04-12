package ca;

import ce.IsolatedPluginClassLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CallbackThreadPoolExecutor extends ThreadPoolExecutor {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackThreadPoolExecutor.class);

    public CallbackThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
    }

    public CallbackThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
    }

    public CallbackThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue,
                                      RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
    }

    public CallbackThreadPoolExecutor(int corePoolSize,
                                      int maximumPoolSize,
                                      long keepAliveTime,
                                      TimeUnit unit,
                                      BlockingQueue<Runnable> workQueue,
                                      ThreadFactory threadFactory,
                                      RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
    }

    protected void beforeExecute(Thread thread, Runnable runnable) {
        super.beforeExecute(thread, runnable);
        if (!(runnable instanceof CallbackFutureTask)) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("CallbackExecutor encountered FutureTask which is not a CallbackFutureTask, using default ClassLoader as fallback");
            }
            thread.setContextClassLoader(getClass().getClassLoader());
            return;
        }
        Object task = ((CallbackFutureTask) runnable).getTask();
        Class clazz;
        if (task instanceof RunnableTask) {
            clazz = ((RunnableTask) task).callbackType();
        } else if (task instanceof CallbackFutureTask) {
            clazz = ((CallableTask) task).callbackType();
        } else {
            thread.setContextClassLoader(getClass().getClassLoader());
            return;
        }
        ClassLoader classLoader = clazz.getClassLoader();
        if (LOGGER.isTraceEnabled() && !(classLoader instanceof IsolatedPluginClassLoader)) {
            LOGGER.trace("Classloader for class {} is not an isolated plugin classloader.", clazz.getCanonicalName());
        }
        thread.setContextClassLoader(classLoader);
    }

    protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
        return new CallbackFutureTask(runnable, value);
    }

    protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {
        return new CallbackFutureTask(callable);
    }

    public static class CallbackFutureTask<V> extends FutureTask<V> {
        private final Object task;

        public CallbackFutureTask(Callable<V> callable) {
            super(callable);
            this.task = callable;
        }

        public CallbackFutureTask(Runnable runnable, V result) {
            super(runnable, result);
            this.task = runnable;
        }

        public Object getTask() {
            return task;
        }
    }
}
