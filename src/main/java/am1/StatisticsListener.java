package am1;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistryListener;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatisticsListener
        implements MetricRegistryListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(StatisticsListener.class);

    public void onGaugeAdded(String paramString, Gauge<?> paramGauge) {
        LOGGER.debug("Statistics gauge [{}] added", paramString);
    }

    public void onGaugeRemoved(String paramString) {
        LOGGER.debug("Statistics gauge [{}] removed", paramString);
    }

    public void onCounterAdded(String paramString, Counter paramCounter) {
        LOGGER.debug("Statistics counter [{}] added", paramString);
    }

    public void onCounterRemoved(String paramString) {
        LOGGER.debug("Statistics counter [{}] removed", paramString);
    }

    public void onHistogramAdded(String paramString, Histogram paramHistogram) {
        LOGGER.debug("Statistics histogram [{}] added", paramString);
    }

    public void onHistogramRemoved(String paramString) {
        LOGGER.debug("Statistics histogram [{}] removed", paramString);
    }

    public void onMeterAdded(String paramString, Meter paramMeter) {
        LOGGER.debug("Statistics meter [{}] added", paramString);
    }

    public void onMeterRemoved(String paramString) {
        LOGGER.debug("Statistics meter [{}] removed", paramString);
    }

    public void onTimerAdded(String paramString, Timer paramTimer) {
        LOGGER.debug("Statistics timer [{}] added", paramString);
    }

    public void onTimerRemoved(String paramString) {
        LOGGER.debug("Statistics timer [{}] removed", paramString);
    }
}
