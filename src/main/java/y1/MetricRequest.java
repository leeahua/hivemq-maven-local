package y1;

import j1.ClusterRequest;

public class MetricRequest implements ClusterRequest {
    private final String metricName;
    private final Class metricClass;

    public MetricRequest(String metricName, Class metricClass) {
        this.metricName = metricName;
        this.metricClass = metricClass;
    }

    public String getMetricName() {
        return metricName;
    }

    public Class getMetricClass() {
        return metricClass;
    }
}
