package u;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.topic.TopicMatcher;

public class MatchTopicFilter implements Filter {
    private final String topic;
    private final TopicMatcher topicMatcher;

    public MatchTopicFilter(String topic, TopicMatcher topicMatcher) {
        this.topic = topic;
        this.topicMatcher = topicMatcher;
    }

    public boolean test(@NotNull String key) {
        Preconditions.checkNotNull(key, "Key must not be null");
        return this.topicMatcher.matches(this.topic, key);
    }
}
