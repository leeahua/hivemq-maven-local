package x1;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterKeyRequest;


public class RetainedMessageGetRequest implements ClusterKeyRequest {
    private final String topic;

    public RetainedMessageGetRequest(@NotNull String topic) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        this.topic = topic;
    }

    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return this.topic;
    }
}
