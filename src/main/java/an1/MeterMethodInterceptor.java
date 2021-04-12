package an1;

import com.codahale.metrics.Meter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class MeterMethodInterceptor implements MethodInterceptor {
    private final Meter meter;

    protected MeterMethodInterceptor(Meter meter) {
        this.meter = meter;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        this.meter.mark();
        return invocation.proceed();
    }
}
