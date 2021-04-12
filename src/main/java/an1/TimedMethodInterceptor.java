package an1;

import com.codahale.metrics.Timer;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

public class TimedMethodInterceptor implements MethodInterceptor {
    private final Timer timer;

    public TimedMethodInterceptor(Timer timer) {
        this.timer = timer;
    }

    public Object invoke(MethodInvocation invocation) throws Throwable {
        Timer.Context timerContext = this.timer.time();
        try {
            return invocation.proceed();
        } finally {
            timerContext.stop();
        }
    }
}
