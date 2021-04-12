package co;

import ai.TopicSubscribers;
import aj.ClusterFutures;
import bu.InternalPublish;
import bx.SubscriberWithQoS;
import by.TopicTreeSinglePersistence;
import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.services.SYSTopicService;
import com.hivemq.spi.topic.sys.SYSTopicEntry;
import com.hivemq.spi.topic.sys.Type;
import i.ClusterIdProducer;
import s.Cluster;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Singleton
public class SYSTopicServiceImpl implements SYSTopicService {
    private final InternalPublishService publishService;
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;
    private final ClusterIdProducer clusterIdProducer;
    private final ListeningExecutorService clusterExecutor;
    private final Map<String, SYSTopicEntry> sysTopicEntries = new ConcurrentSkipListMap<>();

    @Inject
    public SYSTopicServiceImpl(InternalPublishService publishService,
                               TopicTreeSinglePersistence topicTreeSinglePersistence,
                               ClusterIdProducer clusterIdProducer,
                               @Cluster ListeningExecutorService clusterExecutor) {
        this.publishService = publishService;
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        this.clusterIdProducer = clusterIdProducer;
        this.clusterExecutor = clusterExecutor;
    }

    public Collection<SYSTopicEntry> getAllEntries(Type... types) {
        if (types.length == 0) {
            return Collections.unmodifiableCollection(this.sysTopicEntries.values());
        }
        Set<Type> typeSet = Sets.newHashSet(types);
        List<SYSTopicEntry> entries = this.sysTopicEntries.values().stream()
                .filter(Objects::nonNull)
                .filter(typeSet::contains)
                .collect(Collectors.toList());
        return Collections.unmodifiableCollection(entries);
    }

    public boolean contains(SYSTopicEntry entry) {
        return entry != null && this.sysTopicEntries.containsKey(entry.topic());
    }

    public Optional<SYSTopicEntry> getEntry(String topic) {
        if (topic == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.sysTopicEntries.get(topic));
    }

    public void addEntry(SYSTopicEntry entry) {
        Preconditions.checkNotNull(entry, "It's not allowed to pass a null entry to the SYSTopicService");
        Preconditions.checkNotNull(entry.topic(), "It's not allowed to pass a SYSTopicEntry with a null topic");
        Preconditions.checkArgument(entry.topic().startsWith("$SYS/"), "A SYSTopicEntries Topic must start with $SYS. Given: %s", entry.topic());
        this.sysTopicEntries.put(entry.topic(), entry);
    }

    public boolean removeEntry(SYSTopicEntry entry) {
        if (entry != null && entry.topic() != null) {
            return this.sysTopicEntries.remove(entry.topic()) != null;
        }
        return false;
    }

    public void triggerStandardSysTopicPublish() {
        this.sysTopicEntries.forEach((topic, entry) -> {
            ListenableFuture<TopicSubscribers> future = this.topicTreeSinglePersistence.getSubscribers(topic);
            ClusterFutures.addCallback(future, new FutureCallback<TopicSubscribers>() {

                @Override
                public void onSuccess(@Nullable TopicSubscribers result) {
                    if (result.getSubscribers().isEmpty()) {
                        return;
                    }
                    if (entry.type() != Type.STANDARD) {
                        return;
                    }
                    InternalPublish publish = createPublish(topic, entry);
                    result.getSubscribers().forEach(subscriber ->
                            publishService.publishToClient(publish, subscriber.getSubscriber(), clusterExecutor)
                    );
                }

                @Override
                public void onFailure(Throwable t) {
                    t.printStackTrace();
                }
            });
        });
    }

    public void triggerStaticSysTopicPublishToClient(String clientId) {
        this.sysTopicEntries.forEach((topic, entry) -> {
            if (entry.type() == Type.STATIC) {
                publishToClient(clientId, topic, entry);
            }
        });
    }

    public void triggerStandardSysTopicPublishToClient(String clientId) {
        this.sysTopicEntries.forEach((topic, entry) -> {
            if (entry.type() == Type.STANDARD) {
                publishToClient(clientId, topic, entry);
            }
        });
    }

    private void publishToClient(String clientId, String topic, SYSTopicEntry entry) {
        ListenableFuture<TopicSubscribers> future = this.topicTreeSinglePersistence.getSubscribers(topic);
        ClusterFutures.addCallback(future, new FutureCallback<TopicSubscribers>() {

            @Override
            public void onSuccess(@Nullable TopicSubscribers result) {
                Optional<SubscriberWithQoS> maySubscriber = result.getSubscribers().stream()
                        .filter(subscriber -> subscriber.getSubscriber().equals(clientId))
                        .findFirst();
                if (!maySubscriber.isPresent()) {
                    return;
                }
                SubscriberWithQoS subscriber = maySubscriber.get();
                InternalPublish publish = createPublish(topic, entry);
                publishService.publishToClient(publish, subscriber.getSubscriber(), clusterExecutor);
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }


    @NotNull
    private InternalPublish createPublish(String topic, SYSTopicEntry entry) {
        InternalPublish publish = new InternalPublish(this.clusterIdProducer.get());
        publish.setTopic(topic);
        publish.setQoS(QoS.AT_MOST_ONCE);
        publish.setPayload(entry.payload().get());
        return publish;
    }
}
