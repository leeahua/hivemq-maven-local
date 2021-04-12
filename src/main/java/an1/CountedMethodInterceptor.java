package an1;

import com.codahale.metrics.Counter;
import com.hivemq.spi.metrics.annotations.Counted;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class CountedMethodInterceptor implements MethodInterceptor {
    private final Counter counter;
    private final Counted counted;

    public CountedMethodInterceptor(Counter counter, Counted counted) {
        this.counter = counter;
        this.counted = counted;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        this.counter.inc();
        try {
            return invocation.proceed();
        } finally {
            if (!this.counted.monotonic()) {
                this.counter.dec();
            }
        }
    }
}
