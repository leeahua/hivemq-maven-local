package co;

import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.services.AsyncSubscriptionStore;
import com.hivemq.spi.services.BlockingSubscriptionStore;
import d.CacheScoped;

import java.util.Set;

@CacheScoped
public class BlockingSubscriptionStoreImpl implements BlockingSubscriptionStore {
    private final AsyncSubscriptionStore asyncSubscriptionStore;

    @Inject
    public BlockingSubscriptionStoreImpl(AsyncSubscriptionStore asyncSubscriptionStore) {
        this.asyncSubscriptionStore = asyncSubscriptionStore;
    }

    public Multimap<String, Topic> getLocalSubscriptions() {
        try {
            return this.asyncSubscriptionStore.getLocalSubscriptions().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getLocalSubscribers(String topic) {
        try {
            return this.asyncSubscriptionStore.getLocalSubscribers(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Topic> getLocalTopics(String clientId) {
        try {
            return this.asyncSubscriptionStore.getLocalTopics(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addSubscription(String clientId, Topic topic) {
        try {
            this.asyncSubscriptionStore.addSubscription(clientId, topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void removeSubscription(String clientId, String topic) {
        try {
            this.asyncSubscriptionStore.removeSubscription(clientId, topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Multimap<String, Topic> getSubscriptions() {
        try {
            return this.asyncSubscriptionStore.getSubscriptions().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<String> getSubscribers(String topic) {
        try {
            return this.asyncSubscriptionStore.getSubscribers(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<Topic> getTopics(String clientId) {
        try {
            return this.asyncSubscriptionStore.getTopics(clientId).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
