package ao1;

import com.codahale.metrics.Gauge;
import x.RetainedMessagesSinglePersistence;

public class RetainedMessagesCurrent implements Gauge<Long> {
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;

    public RetainedMessagesCurrent(RetainedMessagesSinglePersistence retainedMessagesSinglePersistence) {
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
    }

    @Override
    public Long getValue() {
        try {
            return this.retainedMessagesSinglePersistence.size();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0L;
    }
}
