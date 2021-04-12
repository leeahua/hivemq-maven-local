package co;


import com.codahale.metrics.Gauge;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.message.RetainedMessage;
import com.hivemq.spi.metrics.HiveMQMetrics;
import com.hivemq.spi.services.AsyncMetricService;
import com.hivemq.spi.services.AsyncRetainedMessageStore;
import com.hivemq.spi.services.BlockingMetricService;
import com.hivemq.spi.services.PluginExecutorService;
import d.CacheScoped;
import i.ClusterConfigurationService;
import x.RetainedMessagesClusterPersistence;
import x.RetainedMessagesSinglePersistence;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@CacheScoped
public class AsyncRetainedMessageStoreImpl implements AsyncRetainedMessageStore {
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final RetainedMessagesClusterPersistence retainedMessagesClusterPersistence;
    private final AsyncMetricService asyncMetricService;
    private final BlockingMetricService blockingMetricService;
    private final ClusterConfigurationService clusterConfigurationService;
    private final PluginExecutorService pluginExecutorService;

    @Inject
    public AsyncRetainedMessageStoreImpl(RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                                         RetainedMessagesClusterPersistence retainedMessagesClusterPersistence,
                                         AsyncMetricService asyncMetricService,
                                         BlockingMetricService blockingMetricService,
                                         ClusterConfigurationService clusterConfigurationService,
                                         PluginExecutorService pluginExecutorService) {
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.retainedMessagesClusterPersistence = retainedMessagesClusterPersistence;
        this.asyncMetricService = asyncMetricService;
        this.blockingMetricService = blockingMetricService;
        this.clusterConfigurationService = clusterConfigurationService;
        this.pluginExecutorService = pluginExecutorService;
    }

    public ListenableFuture<Set<RetainedMessage>> getLocalRetainedMessages() {
        ImmutableSet<ListenableFuture<Set<String>>> topicsFutures = this.retainedMessagesClusterPersistence.getLocalWithWildcards("#");
        return getRetainedMessages(topicsFutures);
    }

    public ListenableFuture<Long> localSize() {
        return Futures.immediateFuture(this.blockingMetricService.getHiveMQMetric(HiveMQMetrics.RETAINED_MESSAGES_CURRENT).getValue().longValue());
    }

    public ListenableFuture<Boolean> containsLocally(String paramString) {
        bz.RetainedMessage retainedMessage = this.retainedMessagesClusterPersistence.getLocally(paramString);
        return Futures.immediateFuture(retainedMessage != null);
    }

    public ListenableFuture<Set<RetainedMessage>> getRetainedMessages() {
        ImmutableList<ListenableFuture<Set<String>>> topicsFutures = this.retainedMessagesSinglePersistence.getTopics("#");
        return getRetainedMessages(topicsFutures);
    }

    private ListenableFuture<Set<RetainedMessage>> getRetainedMessages(ImmutableCollection<ListenableFuture<Set<String>>> topicsFutures) {
        SettableFuture<Set<RetainedMessage>> settableFuture = SettableFuture.create();
        ListenableFuture<List<Set<String>>> future = Futures.allAsList(topicsFutures);
        Futures.addCallback(future, new FutureCallback<List<Set<String>>>() {

            @Override
            public void onSuccess(@Nullable List<Set<String>> result) {
                Set<String> topics = result.stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
                List<ListenableFuture<RetainedMessage>> retainedMessageFutures
                        = topics.stream()
                        .map(AsyncRetainedMessageStoreImpl.this::getRetainedMessage)
                        .collect(Collectors.toList());
                ListenableFuture<List<RetainedMessage>> f = Futures.allAsList(retainedMessageFutures);
                Futures.addCallback((ListenableFuture) f, new FutureCallback<List<RetainedMessage>>() {

                    @Override
                    public void onSuccess(@Nullable List<RetainedMessage> result) {
                        settableFuture.set(Sets.newHashSet(result));
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

    public ListenableFuture<RetainedMessage> getRetainedMessage(String topic) {
        return doGetRetainedMessage(topic);
    }

    public ListenableFuture<Void> remove(String topic) {
        return this.retainedMessagesSinglePersistence.remove(topic);
    }

    public ListenableFuture<Void> clear() {
        return this.retainedMessagesClusterPersistence.clear();
    }

    public ListenableFuture<Void> addOrReplace(RetainedMessage retainedMessage) {
        return this.retainedMessagesSinglePersistence.addOrReplace(
                retainedMessage.getTopic(),
                new bz.RetainedMessage(retainedMessage.getMessage(), retainedMessage.getQoS()));
    }

    public ListenableFuture<Boolean> contains(String topic) {
        SettableFuture<Boolean> settableFuture = SettableFuture.create();
        ListenableFuture<RetainedMessage> future = getRetainedMessage(topic);
        Futures.addCallback(future, new FutureCallback<RetainedMessage>() {

            @Override
            public void onSuccess(@Nullable RetainedMessage result) {
                settableFuture.set(result != null);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    public ListenableFuture<Long> size() {
        SettableFuture<Long> settableFuture = SettableFuture.create();
        ListenableFuture<Map<String, Gauge<Number>>> future = this.asyncMetricService.getClusterMetric(HiveMQMetrics.RETAINED_MESSAGES_CURRENT);
        Futures.addCallback(future, new FutureCallback<Map<String, Gauge<Number>>>() {

            @Override
            public void onSuccess(@Nullable Map<String, Gauge<Number>> result) {
                long size = result.values().stream()
                        .mapToLong(gauge -> gauge.getValue().longValue())
                        .sum();
                if (clusterConfigurationService.isEnabled()) {
                    size /= (clusterConfigurationService.getReplicates().getRetainedMessage().getReplicateCount() + 1);
                }
                settableFuture.set(size);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }

    private ListenableFuture<RetainedMessage> doGetRetainedMessage(String topic) {
        SettableFuture<RetainedMessage> settableFuture = SettableFuture.create();
        ListenableFuture<bz.RetainedMessage> future = this.retainedMessagesSinglePersistence.getWithoutWildcards(topic);
        Futures.addCallback(future, new FutureCallback<bz.RetainedMessage>() {

            @Override
            public void onSuccess(@Nullable bz.RetainedMessage result) {
                RetainedMessage retainedMessage = new RetainedMessage(topic, result.getMessage(), result.getQoS());
                settableFuture.set(retainedMessage);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.pluginExecutorService);
        return settableFuture;
    }
}
