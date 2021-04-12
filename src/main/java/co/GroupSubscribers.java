package co;

import bx.SubscriberWithQoS;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class GroupSubscribers {
    private final List<SubscriberWithQoS> subscribers;
    private final AtomicInteger index = new AtomicInteger(0);

    public GroupSubscribers(List<SubscriberWithQoS> subscribers) {
        this.subscribers = subscribers;
    }

    public SubscriberWithQoS next() {
        if (this.subscribers.isEmpty()) {
            return null;
        }
        int index = this.index.incrementAndGet();
        int subscriberIndex = index % this.subscribers.size();
        return this.subscribers.get(subscriberIndex);
    }

    public List<SubscriberWithQoS> getSubscribers() {
        return subscribers;
    }
}
