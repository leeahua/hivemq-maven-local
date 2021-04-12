package aa;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import j1.ClusterKeyRequest;

public class TopicTreeRemoveReplicateRequest
        implements ClusterKeyRequest {
    @NotNull
    private final String segmentKey;
    @NotNull
    private final String topic;
    @NotNull
    private final String subscriber;

    public TopicTreeRemoveReplicateRequest(@NotNull String segmentKey, @NotNull String topic, @NotNull String subscriber) {
        this.segmentKey = segmentKey;
        this.topic = topic;
        this.subscriber = subscriber;
        Preconditions.checkNotNull(segmentKey, "Segment Key must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
    }

    @NotNull
    public String getSegmentKey() {
        return segmentKey;
    }

    @NotNull
    public String getTopic() {
        return topic;
    }

    @NotNull
    public String getSubscriber() {
        return subscriber;
    }

    public String getKey() {
        return this.segmentKey;
    }
}
