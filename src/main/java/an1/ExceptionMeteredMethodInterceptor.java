package an1;

import com.codahale.metrics.Meter;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class ExceptionMeteredMethodInterceptor implements MethodInterceptor {
    private final Meter meter;
    private final Class<? extends Throwable> exception;

    protected ExceptionMeteredMethodInterceptor(Meter meter, Class<? extends Throwable> exception) {
        this.meter = meter;
        this.exception = exception;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        try {
            return invocation.proceed();
        } catch (Throwable e) {
            if (this.exception.isAssignableFrom(e.getClass())) {
                this.meter.mark();
            }
            throw e;
        }
    }
}
