package bx;

import com.hivemq.spi.topic.TopicMatcher;
import com.hivemq.spi.topic.exception.InvalidTopicException;
import org.apache.commons.lang3.StringUtils;

public class PublishTopicMatcher implements TopicMatcher {
    public boolean matches(String topicSubscription, String actualTopic) {
        if (StringUtils.containsAny(actualTopic, "#+")) {
            throw new InvalidTopicException("The actual topic must not contain a wildard character (# or +)");
        }
        String subscription = StringUtils.stripEnd(topicSubscription, "/");
        String topic = actualTopic;
        if (actualTopic.length() > 1) {
            topic = StringUtils.stripEnd(actualTopic, "/");
        }
        if (StringUtils.containsNone(topicSubscription, "#+")) {
            return subscription.equals(topic);
        }
        if (actualTopic.startsWith("$") && !topicSubscription.startsWith("$")) {
            return false;
        }
        return matchesWildcards(subscription, topic);
    }

    private static boolean matchesWildcards(String topicSubscription, String actualTopic) {
        if (topicSubscription.contains("#") &&
                !StringUtils.endsWith(topicSubscription, "/#") &&
                topicSubscription.length() > 1) {
            return false;
        }
        String[] subscriptionArray = StringUtils.splitPreserveAllTokens(topicSubscription, "/");
        String[] topicArray = StringUtils.splitPreserveAllTokens(actualTopic, "/");
        int smallest = Math.min(subscriptionArray.length, topicArray.length);
        for (int index = 0; index < smallest; index++) {
            String sub = subscriptionArray[index];
            String t = topicArray[index];
            if (!sub.equals(t)) {
                if (sub.equals("#")) {
                    return true;
                }
                if (!sub.equals("+")) {
                    return false;
                }
            }
        }
        return subscriptionArray.length == topicArray.length ||
                (subscriptionArray.length - topicArray.length == 1 &&
                        subscriptionArray[(subscriptionArray.length - 1)].equals("#"));
    }
}
