package a;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.hivemq.spi.aop.cache.Cached;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class MethodCacheInterceptor implements MethodInterceptor {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCacheInterceptor.class);
    private final ConcurrentMap<String, Cache<Object, Object>> caches;

    public MethodCacheInterceptor(ConcurrentMap<String, Cache<Object, Object>> caches) {
        this.caches = caches;
    }

    public Object invoke(MethodInvocation invocation) throws ExecutionException {
        String cacheKey = createCacheKey(invocation);
        Cached cached = invocation.getStaticPart().getAnnotation(Cached.class);
        this.caches.putIfAbsent(invocation.getMethod().toString(),
                CacheBuilder.newBuilder()
                        .expireAfterWrite(cached.timeToLive(), cached.timeUnit()).build());
        Cache<Object, Object> cache = this.caches.get(invocation.getMethod().toString());
        Object value = cache.get(cacheKey, () -> {
            try {
                return invoke(invocation);
            } catch (Throwable e) {
                LOGGER.debug("Problem when trying to get value from @Cached cache", e);
                throw new RuntimeException(e);
            }
        });
        return value;
    }

    @VisibleForTesting
    String createCacheKey(MethodInvocation invocation) {
        StringBuilder builder = new StringBuilder();
        builder.append(invocation.getThis().getClass().getSuperclass());
        builder.append("#");
        builder.append(invocation.getMethod().getName());
        builder.append("[");
        List<Object> arguments = Arrays.stream(invocation.getArguments())
                .map(argument -> {
                    if (argument == null) {
                        return "null";
                    }
                    return argument.hashCode();
                })
                .collect(Collectors.toList());
        builder.append(Joiner.on(",").join(arguments));
        builder.append("]");
        return builder.toString();
    }
}
