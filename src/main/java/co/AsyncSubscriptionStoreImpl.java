package co;

import ai.TopicSubscribers;
import aj.ClusterFutures;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import bx.SubscriberWithQoS;
import by.TopicTreeClusterPersistence;
import by.TopicTreeSinglePersistence;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.services.AsyncSubscriptionStore;
import com.hivemq.spi.services.PluginExecutorService;
import d.CacheScoped;
import v.ClientSessionClusterPersistence;
import y.ClientSessionSubscriptionsClusterPersistence;
import y.ClientSubscriptions;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@CacheScoped
public class AsyncSubscriptionStoreImpl implements AsyncSubscriptionStore {
    private final ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence;
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;
    private final TopicTreeClusterPersistence topicTreeClusterPersistence;
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final PluginExecutorService pluginExecutorService;

    @Inject
    public AsyncSubscriptionStoreImpl(ClientSessionSubscriptionsClusterPersistence clientSessionSubscriptionsClusterPersistence,
                                      ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
                                      TopicTreeSinglePersistence topicTreeSinglePersistence,
                                      TopicTreeClusterPersistence topicTreeClusterPersistence,
                                      ClientSessionClusterPersistence clientSessionClusterPersistence,
                                      PluginExecutorService pluginExecutorService) {
        this.clientSessionSubscriptionsClusterPersistence = clientSessionSubscriptionsClusterPersistence;
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        this.topicTreeClusterPersistence = topicTreeClusterPersistence;
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.pluginExecutorService = pluginExecutorService;
    }

    public ListenableFuture<Multimap<String, Topic>> getLocalSubscriptions() {
        ListenableFuture<Set<String>> clientsFuture = this.clientSessionClusterPersistence.getLocalAllClients();
        return getSubscriptions(clientsFuture);
    }

    public ListenableFuture<Set<String>> getLocalSubscribers(String topic) {
        return Futures.immediateFuture(doGetLocalSubscribers(topic));
    }

    private Set<String> doGetLocalSubscribers(String topic) {
        ImmutableSet<SubscriberWithQoS> subscribers = this.topicTreeClusterPersistence.getLocalSubscribers(topic);
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        subscribers.forEach(subscriber -> builder.add(subscriber.getSubscriber()));
        return builder.build();
    }

    public ListenableFuture<Set<Topic>> getLocalTopics(String clientId) {
        return Futures.immediateFuture(getClientSubscriptions(clientId));
    }

    private Set<Topic> getClientSubscriptions(String clientId) {
        ClientSubscriptions clientSubscriptions = this.clientSessionSubscriptionsClusterPersistence.getClientSubscriptions(clientId);
        return clientSubscriptions.getSubscriptions();
    }

    public ListenableFuture<Void> addSubscription(String clientId, Topic topic) {
        return this.clientSessionSubscriptionsSinglePersistence.addSubscription(clientId, topic);
    }

    public ListenableFuture<Void> removeSubscription(String clientId, String topic) {
        return this.clientSessionSubscriptionsSinglePersistence.removeSubscription(clientId, Topic.topicFromString(topic));
    }

    public ListenableFuture<Multimap<String, Topic>> getSubscriptions() {
        ListenableFuture<Set<String>> clientsFuture = this.clientSessionClusterPersistence.getAllClients();
        return getSubscriptions(clientsFuture);
    }

    private ListenableFuture<Multimap<String, Topic>> getSubscriptions(ListenableFuture<Set<String>> clientsFuture) {
        SettableFuture<Multimap<String, Topic>> settableFuture = SettableFuture.create();
        ImmutableMultimap.Builder<String, Topic> builder = ImmutableMultimap.builder();
        List<ListenableFuture<Void>> futures = new ArrayList<>();
        Futures.addCallback(clientsFuture, new FutureCallback<Set<String>>() {

            @Override
            public void onSuccess(@Nullable Set<String> result) {
                result.forEach(clientId -> {
                    SettableFuture<Void> clientSettableFuture = SettableFuture.create();
                    futures.add(clientSettableFuture);
                    ListenableFuture<ClientSubscriptions> clientSubscriptionsFuture = clientSessionSubscriptionsSinglePersistence.getSubscriptions(clientId);
                    Futures.addCallback(clientSubscriptionsFuture, new FutureCallback<ClientSubscriptions>() {

                        @Override
                        public void onSuccess(@Nullable ClientSubscriptions result) {
                            builder.putAll(clientId, result.getSubscriptions());
                            clientSettableFuture.set(null);
                        }

                        @Override
                        public void onFailure(Throwable t) {
                            settableFuture.setException(t);
                        }
                    }, pluginExecutorService);
                });
                ListenableFuture<Void> future = ClusterFutures.merge(futures);
                Futures.addCallback(future, new FutureCallback<Void>() {

                    @Override
                    public void onSuccess(@Nullable Void result) {
                        settableFuture.set(builder.build());
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        settableFuture.setException(t);
                    }
                }, pluginExecutorService);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Set<String>> getSubscribers(String topic) {
        ListenableFuture<TopicSubscribers> future = this.topicTreeSinglePersistence.getSubscribers(topic);
        SettableFuture<Set<String>> settableFuture = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<TopicSubscribers>() {

            @Override
            public void onSuccess(@Nullable TopicSubscribers result) {
                ImmutableSet.Builder<String> builder = ImmutableSet.builder();
                result.getSubscribers().stream()
                        .map(SubscriberWithQoS::getSubscriber)
                        .forEach(builder::add);
                settableFuture.set(builder.build());
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Set<Topic>> getTopics(String clientId) {
        ListenableFuture<ClientSubscriptions> future = this.clientSessionSubscriptionsSinglePersistence.getSubscriptions(clientId);
        SettableFuture<Set<Topic>> setSettableFuture = SettableFuture.create();
        Futures.addCallback(future, new FutureCallback<ClientSubscriptions>() {

            @Override
            public void onSuccess(@Nullable ClientSubscriptions result) {
                setSettableFuture.set(result.getSubscriptions());
            }

            @Override
            public void onFailure(Throwable t) {
                setSettableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return setSettableFuture;
    }
}
