package bp1;

import cb.EventBusSubscribers;
import cd.QuartzCallbackScheduler;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.primitives.Ints;
import com.hivemq.spi.callback.AsynchronousCallback;
import com.hivemq.spi.callback.Callback;
import com.hivemq.spi.callback.SynchronousCallback;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.callback.schedule.ScheduledCallback;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class CallbackRegistryImpl implements CallbackRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(CallbackRegistryImpl.class);
    private final ListMultimap<Class<? extends Callback>, Callback> callbacks;
    private final Provider<QuartzCallbackScheduler> schedulerProvider;
    private final Provider<EventBusSubscribers> subscribersProvider;
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(false);
    private boolean subscribersInitialized = false;

    @Inject
    CallbackRegistryImpl(Provider<QuartzCallbackScheduler> schedulerProvider,
                         Provider<EventBusSubscribers> subscribersProvider) {
        this.schedulerProvider = schedulerProvider;
        this.subscribersProvider = subscribersProvider;
        this.callbacks = LinkedListMultimap.create();
    }

    public void addCallback(Callback callback) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            if (!this.subscribersInitialized) {
                this.subscribersProvider.get();
                this.subscribersInitialized = true;
            }
            Class<?> callbackType = callback.getClass();
            List<Class<? extends Callback>> allInterfaces = (List<Class<? extends Callback>>) ClassUtils.getAllInterfaces(callbackType);
            allInterfaces.forEach(interfaceClass -> {
                if (!interfaceClass.equals(Callback.class) &&
                        !interfaceClass.equals(SynchronousCallback.class) &&
                        !interfaceClass.equals(AsynchronousCallback.class) &&
                        (SynchronousCallback.class.isAssignableFrom(interfaceClass) ||
                                AsynchronousCallback.class.isAssignableFrom(interfaceClass))) {
                    this.callbacks.put(interfaceClass, callback);
                    LOGGER.debug("Callback {} added to CallbackRegistry for interface {}", callbackType, interfaceClass);
                    if ((callback instanceof ScheduledCallback)) {
                        startScheduling((ScheduledCallback) callback);
                    }
                }
                Collections.sort(this.callbacks.get(interfaceClass), getComparator());
            });
        } finally {
            lock.unlock();
        }
    }

    private void startScheduling(ScheduledCallback callback) {
        LOGGER.info("Scheduling callback {}: {}", callback.getClass().getName(), callback.cronExpression());
        synchronized (this.schedulerProvider) {
            this.schedulerProvider.get().schedule(callback);
        }
    }

    private void stopScheduling(ScheduledCallback callback) {
        synchronized (this.schedulerProvider) {
            this.schedulerProvider.get().unschedule(callback);
        }
    }

    public void addCallbacks(Callback... callbacks) {
        for (Callback localCallback : callbacks) {
            addCallback(localCallback);
        }
    }

    public <T extends Callback> List<T> getCallbacks(Class<T> callbackClass) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            List<T> callbacks = (List<T>) this.callbacks.get(callbackClass);
            return ImmutableList.copyOf(callbacks);
        } finally {
            lock.unlock();
        }
    }

    public <T extends Callback> boolean isAvailable(Class<T> callbackClass) {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return this.callbacks.containsKey(callbackClass);
        } finally {
            lock.unlock();
        }
    }

    public Set<Class<? extends Callback>> getAllRegisteredCallbackClasses() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableSet.copyOf(this.callbacks.keySet());
        } finally {
            lock.unlock();
        }
    }

    public void removeCallback(Callback callback) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            Class<? extends Callback> type = callback.getClass();
            List<Class<?>> interfaceTypes = ClassUtils.getAllInterfaces(type);
            interfaceTypes.stream()
                    .filter(interfaceType -> !interfaceType.equals(Callback.class))
                    .filter(interfaceType -> !interfaceType.equals(SynchronousCallback.class))
                    .filter(interfaceType -> !interfaceType.equals(AsynchronousCallback.class))
                    .filter(interfaceType ->
                            SynchronousCallback.class.isAssignableFrom(interfaceType) ||
                                    AsynchronousCallback.class.isAssignableFrom(interfaceType))
                    .forEach(callbackClass -> {
                        this.callbacks.remove(callbackClass, callback);
                        LOGGER.debug("Callback {} removed to CallbackRegistry for interface {}", type, callbackClass);
                        if (callback instanceof ScheduledCallback) {
                            stopScheduling((ScheduledCallback) callback);
                        }
                    });
        } finally {
            lock.unlock();
        }
    }

    public void removeAllCallbacks(Class<? extends Callback> callbackClass) {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            LOGGER.debug("Removing all Callbacks of Type {}", callbackClass);
            this.callbacks.removeAll(callbackClass);
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        Lock lock = this.readWriteLock.writeLock();
        lock.lock();
        try {
            this.callbacks.clear();
            LOGGER.debug("Callback Registry cleared");
        } finally {
            lock.unlock();
        }
    }

    public Set<Callback> getAllCallbacks() {
        Lock lock = this.readWriteLock.readLock();
        lock.lock();
        try {
            return ImmutableSet.copyOf(this.callbacks.values());
        } finally {
            lock.unlock();
        }
    }

    public void reloadScheduledCallbackExpression(ScheduledCallback callback) {
        LOGGER.info("Reloading cron expression of callback {}", callback);
        stopScheduling(callback);
        startScheduling(callback);
    }

    @VisibleForTesting
    protected Comparator<Callback> getComparator() {
        return (callback1, callback2) -> {
            if (callback1 instanceof SynchronousCallback &&
                    callback2 instanceof SynchronousCallback) {
                return Ints.compare(((SynchronousCallback) callback1).priority(),
                        ((SynchronousCallback) callback2).priority());
            }
            if (callback1 instanceof AsynchronousCallback &&
                    callback2 instanceof SynchronousCallback) {
                return 1;
            }
            if (callback1 instanceof SynchronousCallback &&
                    callback2 instanceof AsynchronousCallback) {
                return -1;
            }
            if (callback1 instanceof AsynchronousCallback &&
                    callback2 instanceof AsynchronousCallback) {
                return -1;
            }
            throw new IllegalStateException();
        };
    }
}
