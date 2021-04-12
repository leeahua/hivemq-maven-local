package ai;

import bx.SubscriberWithQoS;
import com.google.common.collect.ImmutableSet;

public class TopicSubscribers {
    private final ImmutableSet<SubscriberWithQoS> subscribers;

    public TopicSubscribers(ImmutableSet<SubscriberWithQoS> subscribers) {
        this.subscribers = subscribers;
    }

    public ImmutableSet<SubscriberWithQoS> getSubscribers() {
        return subscribers;
    }
}
