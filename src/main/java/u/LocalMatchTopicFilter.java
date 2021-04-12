package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.topic.TopicMatcher;
import q.ConsistentHashingRing;

public class LocalMatchTopicFilter implements Filter {
    private final String topic;
    private final TopicMatcher topicMatcher;
    private final ConsistentHashingRing ring;
    private final String clusterId;

    public LocalMatchTopicFilter(String topic,
                                 TopicMatcher topicMatcher,
                                 ConsistentHashingRing ring,
                                 String clusterId) {
        this.topic = topic;
        this.topicMatcher = topicMatcher;
        this.ring = ring;
        this.clusterId = clusterId;
    }

    @Override
    public boolean test(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        return this.topicMatcher.matches(this.topic, key) &&
                this.ring.getNode(key).equals(this.clusterId);
    }
}
