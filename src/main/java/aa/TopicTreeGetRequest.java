package aa;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterKeyRequest;

public class TopicTreeGetRequest implements ClusterKeyRequest {
    @NotNull
    private final String segmentKey;
    @NotNull
    private final String topic;

    public TopicTreeGetRequest(@NotNull String segmentKey, @NotNull String topic) {
        this.segmentKey = segmentKey;
        this.topic = topic;
        Preconditions.checkNotNull(segmentKey, "Segment Key must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
    }

    @NotNull
    public String getSegmentKey() {
        return segmentKey;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    public String getKey() {
        return this.segmentKey;
    }
}
