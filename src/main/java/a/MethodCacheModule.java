package a;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.hivemq.spi.aop.cache.Cached;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;

public class MethodCacheModule extends AbstractModule {
    private static final Logger LOGGER = LoggerFactory.getLogger(MethodCacheModule.class);

    @Override
    protected void configure() {
        LOGGER.debug("Enabled method caches");
        bindInterceptor(Matchers.any(),
                Matchers.annotatedWith(Cached.class),
                new MethodCacheInterceptor(new ConcurrentHashMap<>()));
    }
}
