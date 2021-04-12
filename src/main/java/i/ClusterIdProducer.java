package i;

import com.google.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;


@Singleton
public class ClusterIdProducer {
    private final String clusterId = RandomStringUtils.randomAlphanumeric(5);

    public String get() {
        return this.clusterId;
    }
}
