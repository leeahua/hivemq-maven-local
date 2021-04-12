package co;

import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.services.AsyncSubscriptionStore;
import com.hivemq.spi.services.BlockingSubscriptionStore;
import com.hivemq.spi.services.SubscriptionStore;
import d.CacheScoped;

import java.util.Set;

@CacheScoped
public class SubscriptionStoreImpl
        implements SubscriptionStore {
    private final AsyncSubscriptionStore asyncSubscriptionStore;
    private final BlockingSubscriptionStore blockingSubscriptionStore;

    @Inject
    public SubscriptionStoreImpl(AsyncSubscriptionStore asyncSubscriptionStore,
                                 BlockingSubscriptionStore blockingSubscriptionStore) {
        this.asyncSubscriptionStore = asyncSubscriptionStore;
        this.blockingSubscriptionStore = blockingSubscriptionStore;
    }

    public Multimap<String, Topic> getLocalSubscriptions() {
        return this.blockingSubscriptionStore.getLocalSubscriptions();
    }

    public Set<String> getLocalSubscribers(String topic) {
        return this.blockingSubscriptionStore.getLocalSubscribers(topic);
    }

    public Set<Topic> getLocalTopics(String clientId) {
        return this.blockingSubscriptionStore.getLocalTopics(clientId);
    }

    public ListenableFuture<Void> addSubscription(String clientId, Topic topic) {
        return this.asyncSubscriptionStore.addSubscription(clientId, topic);
    }

    public ListenableFuture<Void> removeSubscription(String clientId, String topic) {
        return this.asyncSubscriptionStore.removeSubscription(clientId, topic);
    }

    public ListenableFuture<Multimap<String, Topic>> getSubscriptions() {
        return this.asyncSubscriptionStore.getSubscriptions();
    }

    public ListenableFuture<Set<String>> getSubscribers(String topic) {
        return this.asyncSubscriptionStore.getSubscribers(topic);
    }

    public ListenableFuture<Set<Topic>> getTopics(String clientId) {
        return this.asyncSubscriptionStore.getTopics(clientId);
    }
}
