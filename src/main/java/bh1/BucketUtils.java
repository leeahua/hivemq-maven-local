package bh1;

import com.hivemq.spi.annotations.NotNull;

public class BucketUtils {
    public static int bucket(@NotNull String clientId, int bucketCount) {
        return Math.abs(clientId.hashCode() % bucketCount);
    }
}
