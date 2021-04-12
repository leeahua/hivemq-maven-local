package aa;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.Topic;
import j1.ClusterKeyRequest;

public class TopicTreeReplicateAddRequest
        implements ClusterKeyRequest {
    @NotNull
    private final Topic topic;
    @NotNull
    private final String subscriber;
    @NotNull
    private final String segmentKey;
    private final byte shared;
    @Nullable
    private final String groupId;

    public TopicTreeReplicateAddRequest(@NotNull Topic topic,
                                        @NotNull String subscriber,
                                        @NotNull String segmentKey,
                                        byte shared,
                                        @Nullable String groupId) {
        this.topic = topic;
        this.subscriber = subscriber;
        this.segmentKey = segmentKey;
        this.shared = shared;
        this.groupId = groupId;
        Preconditions.checkNotNull(topic, "Topic must not be null");
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
        Preconditions.checkNotNull(segmentKey, "Segment Key must not be null");
    }

    @NotNull
    public Topic getTopic() {
        return topic;
    }

    @NotNull
    public String getSubscriber() {
        return subscriber;
    }

    @NotNull
    public String getSegmentKey() {
        return segmentKey;
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
