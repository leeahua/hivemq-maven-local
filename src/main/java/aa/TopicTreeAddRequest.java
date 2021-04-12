package aa;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import j1.ClusterKeyRequest;

public class TopicTreeAddRequest implements ClusterKeyRequest {
    @NotNull
    private final String segmentKey;
    @NotNull
    private final Topic topic;
    @NotNull
    private final String subscriber;
    private final byte shared;
    @Nullable
    private final String groupId;

    public TopicTreeAddRequest(
            @NotNull String segmentKey,
            @NotNull Topic topic,
            @NotNull String subscriber,
            byte shared,
            @Nullable String groupId) {
        this.segmentKey = segmentKey;
        this.topic = topic;
        this.subscriber = subscriber;
        this.shared = shared;
        this.groupId = groupId;
        Preconditions.checkNotNull(segmentKey, "Segment Key must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
    }

    @NotNull
    public String getSegmentKey() {
        return segmentKey;
    }

    @NotNull
    public Topic getTopic() {
        return topic;
    }

    @NotNull
    public String getSubscriber() {
        return subscriber;
    }

    public byte getShared() {
        return shared;
    }

    @Nullable
    public String getGroupId() {
        return groupId;
    }

    public String getKey() {
        return this.segmentKey;
    }
}
