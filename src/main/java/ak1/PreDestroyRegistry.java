package ak1;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.spi.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class PreDestroyRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(PreDestroyRegistry.class);
    private final List<PreDestroyHolder> holders;

    PreDestroyRegistry() {
        this.holders = Collections.synchronizedList(new ArrayList<>());
    }

    public void add(@NotNull Method method, @NotNull Object injectee) {
        Preconditions.checkNotNull(method);
        Preconditions.checkNotNull(injectee);
        this.holders.add(new PreDestroyHolder(method, injectee));
    }

    public ListenableFuture<?> destroy() {
        ExecutorService delegate = Executors.newFixedThreadPool(3,
                new ThreadFactoryBuilder().setNameFormat("PreDestroy-%d").build());
        ListeningExecutorService executorService = MoreExecutors.listeningDecorator(delegate);
        List<ListenableFuture<?>> futures = this.holders.stream()
                .map(holder -> executorService.submit(() -> {
                    try {
                        holder.getMethod().invoke(holder.getInjectee(), new Object[0]);
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        LOGGER.error("Could not execute preDestroy method for class {}", holder.getInjectee().getClass(), e);
                    }
                }))
                .collect(Collectors.toList());
        return Futures.allAsList(futures);
    }

    private static final class PreDestroyHolder {
        private final Method method;
        private final Object injectee;

        public PreDestroyHolder(@NotNull Method method, @NotNull Object injectee) {
            this.method = method;
            this.injectee = injectee;
        }

        public Method getMethod() {
            return method;
        }

        public Object getInjectee() {
            return injectee;
        }
    }
}
