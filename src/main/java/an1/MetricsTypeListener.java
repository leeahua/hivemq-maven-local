package an1;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.metrics.annotations.Counted;
import com.hivemq.spi.metrics.annotations.ExceptionMetered;
import com.hivemq.spi.metrics.annotations.Metered;
import com.hivemq.spi.metrics.annotations.Timed;
import org.aopalliance.intercept.MethodInterceptor;

import java.lang.reflect.Method;

public class MetricsTypeListener implements TypeListener {
    public static final String EXCEPTION_METERED_PREFIX = "exception-metered";
    public static final String COUNTED_PREFIX = "counted";
    public static final String METERED_PREFIX = "metered";
    public static final String TIMED_PREFIX = "timed";
    private final MetricRegistry metricRegistry;

    public MetricsTypeListener(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    public <T> void hear(TypeLiteral<T> type, TypeEncounter<T> encounter) {
        Class<? super T> rawType = type.getRawType();
        do {
            for (Method method : rawType.getDeclaredMethods()) {
                hear(method, encounter);
            }
        }
        while ((rawType = rawType.getSuperclass()) != null);
    }

    private void hear(Method method, TypeEncounter encounter) {
        MethodInterceptor countedMethodInterceptor = countedMethodInterceptor(method);
        if (countedMethodInterceptor != null) {
            encounter.bindInterceptor(Matchers.only(method), countedMethodInterceptor);
        }
        MethodInterceptor exceptionMeteredMethodInterceptor = exceptionMeteredMethodInterceptor(method);
        if (exceptionMeteredMethodInterceptor != null) {
            encounter.bindInterceptor(Matchers.only(method), exceptionMeteredMethodInterceptor);
        }

        MethodInterceptor meteredMethodInterceptor = meteredMethodInterceptor(method);
        if (meteredMethodInterceptor != null) {
            encounter.bindInterceptor(Matchers.only(method), meteredMethodInterceptor);
        }

        MethodInterceptor timedMethodInterceptor = timedMethodInterceptor(method);
        if (timedMethodInterceptor != null) {
            encounter.bindInterceptor(Matchers.only(method), timedMethodInterceptor);
        }
    }


    @Nullable
    private MethodInterceptor countedMethodInterceptor(Method method) {
        Counted counted = method.getAnnotation(Counted.class);
        if (counted == null) {
            return null;
        }
        Counter counter = this.metricRegistry.counter(buildName(COUNTED_PREFIX, method, counted.name()));
        return new CountedMethodInterceptor(counter, counted);
    }

    @Nullable
    private MethodInterceptor exceptionMeteredMethodInterceptor(Method method) {
        ExceptionMetered exceptionMetered = method.getAnnotation(ExceptionMetered.class);
        if (exceptionMetered == null) {
            return null;
        }
        Meter meter = this.metricRegistry.meter(buildName(EXCEPTION_METERED_PREFIX, method, exceptionMetered.name()));
        return new ExceptionMeteredMethodInterceptor(meter, exceptionMetered.cause());
    }

    @Nullable
    private MethodInterceptor meteredMethodInterceptor(Method method) {
        Metered metered = method.getAnnotation(Metered.class);
        if (metered == null) {
            return null;
        }
        Meter meter = this.metricRegistry.meter(buildName(METERED_PREFIX, method, metered.name()));
        return new MeterMethodInterceptor(meter);
    }

    @Nullable
    private MethodInterceptor timedMethodInterceptor(Method method) {
        Timed timed = method.getAnnotation(Timed.class);
        if (timed == null) {
            return null;
        }
        Timer timer = this.metricRegistry.timer(buildName(TIMED_PREFIX, method, timed.name()));
        return new TimedMethodInterceptor(timer);
    }

    @NotNull
    private String buildName(String prefix, Method method, String annotatedName) {
        if (annotatedName.trim().length() < 1) {
            return buildName(prefix, method);
        }
        return annotatedName;
    }

    @NotNull
    private String buildName(String prefix, Method method) {
        return "com.hivemq.plugins." + prefix + "." + buildName(method);
    }

    @NotNull
    private String buildName(Method method) {
        return method.getDeclaringClass().getCanonicalName() + "." + method.getName();
    }
}
