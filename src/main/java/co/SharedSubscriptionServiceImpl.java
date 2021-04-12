package co;

import av.HiveMQConfigurationService;
import av.Internals;
import bl1.ChannelPersistence;
import bx.SubscriberWithQoS;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.services.SharedSubscriptionService;
import d.CacheScoped;
import i.ClusterConfigurationService;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@ThreadSafe
@CacheScoped
public class SharedSubscriptionServiceImpl implements SharedSubscriptionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(SharedSubscriptionServiceImpl.class);
    private static final String DEFAULT = "default";
    private final Set<String> sharedSubscriptions = new ConcurrentHashSet<>();
    private final Pattern pattern = Pattern.compile("\\$share:(.*?):(.*)");
    private final Map<String, AtomicLong> groupIndexes = new ConcurrentHashMap<>();
    private final Random random = new Random();
    private final ChannelPersistence channelPersistence;
    private final ClusterConfigurationService clusterConfigurationService;
    private final HiveMQConfigurationService hiveMQConfigurationService;
    private Cache<String, GroupSubscribers> groupSubscribersCache;
    private int sharedSubscriptionCacheProbability;

    @Inject
    public SharedSubscriptionServiceImpl(ChannelPersistence channelPersistence,
                                         ClusterConfigurationService clusterConfigurationService,
                                         HiveMQConfigurationService hiveMQConfigurationService) {
        this.channelPersistence = channelPersistence;
        this.clusterConfigurationService = clusterConfigurationService;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
    }

    @PostConstruct
    protected void init() {
        this.groupSubscribersCache = CacheBuilder.newBuilder()
                .expireAfterWrite(this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.SHARED_SUBSCRIPTION_CACHE_DURATION), TimeUnit.SECONDS)
                .recordStats()
                .concurrencyLevel(this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.CLUSTER_THREAD_POOL_SIZE_MAXIMUM))
                .build();
        this.sharedSubscriptionCacheProbability = this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.SHARED_SUBSCRIPTION_CACHE_PROBABILITY);
        List<String> sharedSubscriptions = this.hiveMQConfigurationService.sharedSubscriptionsConfiguration().getSharedSubscriptions();
        sharedSubscriptions.forEach(this::addSharedSubscriptions);
    }

    public void addSharedSubscriptions(@NotNull String... sharedSubscriptions) {
        for (String sharedSubscription : sharedSubscriptions) {
            if (sharedSubscription == null) {
                LOGGER.warn("Ignoring shared subscription with null value");
            } else {
                this.sharedSubscriptions.add(sharedSubscription);
            }
        }
    }

    @Deprecated
    public boolean sharedSubscriptionsAvailable() {
        return this.sharedSubscriptions.size() > 0;
    }

    public void removeSharedSubscription(@NotNull String sharedSubscription) {
        this.sharedSubscriptions.remove(sharedSubscription);
    }

    public List<String> getSharedSubscriptions() {
        return ImmutableList.copyOf(this.sharedSubscriptions);
    }

    public long getSharedSubscriptionsSize() {
        return this.sharedSubscriptions.size();
    }

    public boolean contains(String sharedSubscription) {
        return this.sharedSubscriptions.contains(sharedSubscription);
    }

    public long nextIndex(String groupId) {
        AtomicLong index = this.groupIndexes.get(groupId);
        if (index == null) {
            index = new AtomicLong();
            this.groupIndexes.put(groupId, index);
        }
        return index.getAndIncrement();
    }

    @Nullable
    public SharedTopic getSharedTopic(String sharedTopic) {
        Matcher matcher = this.pattern.matcher(sharedTopic);
        if (matcher.matches()) {
            String groupId = matcher.group(1);
            String topic = matcher.group(2);
            return new SharedTopic(topic, groupId);
        }
        if (this.sharedSubscriptions.contains(sharedTopic)) {
            return new SharedTopic(sharedTopic, DEFAULT);
        }
        return null;
    }


    public SubscriberWithQoS nextSubscriber(List<SubscriberWithQoS> publishedSubscribers,
                                            List<SubscriberWithQoS> groupSubscribers,
                                            String groupId) {
        SubscriberWithQoS result;
        if (this.clusterConfigurationService.isEnabled() &&
                this.random.nextInt(100) + 1 <= this.sharedSubscriptionCacheProbability) {
            GroupSubscribers sharedSubscriptionSubscribers = this.groupSubscribersCache.getIfPresent(groupId);
            if (sharedSubscriptionSubscribers == null) {
                sharedSubscriptionSubscribers = getPersistSubscribers(groupSubscribers);
                this.groupSubscribersCache.put(groupId, sharedSubscriptionSubscribers);
            }
            SubscriberWithQoS next = sharedSubscriptionSubscribers.next();
            if (next != null && groupSubscribers.contains(next)) {
                result = next;
            } else {
                result = nextSubscriber(groupId, groupSubscribers);
            }
        } else {
            result = nextSubscriber(groupId, groupSubscribers);
        }
        if (publishedSubscribers.contains(result) &&
                publishedSubscribers.size() < groupSubscribers.size()) {
            return nextSubscriber(publishedSubscribers, groupSubscribers, groupId);
        }
        return result;
    }


    public void addSubscriberToGroup(SubscriberWithQoS subscriber, String groupId) {
        GroupSubscribers groupSubscribers = this.groupSubscribersCache.getIfPresent(groupId);
        if (groupSubscribers != null &&
                !groupSubscribers.getSubscribers().contains(subscriber)) {
            groupSubscribers.getSubscribers().add(subscriber);
        }
    }


    public void removeSubscriberFromGroup(SubscriberWithQoS subscriber, String groupId) {
        GroupSubscribers groupSubscribers =
                this.groupSubscribersCache.getIfPresent(groupId);
        if (groupSubscribers != null) {
            groupSubscribers.getSubscribers().remove(subscriber);
        }
    }

    private SubscriberWithQoS nextSubscriber(String groupId, List<SubscriberWithQoS> subscribers) {
        long index = nextIndex(groupId);
        int subscriberIndex = (int) (index % subscribers.size());
        return subscribers.get(subscriberIndex);
    }

    private GroupSubscribers getPersistSubscribers(List<SubscriberWithQoS> subscribers) {
        List<SubscriberWithQoS> existsSubscribers = subscribers.stream()
                .filter(subscriber -> Objects.nonNull(
                        this.channelPersistence.getChannel(subscriber.getSubscriber())))
                .collect(Collectors.toList());
        return new GroupSubscribers(existsSubscribers);
    }


    public static class SharedTopic {
        private final String topic;
        private final String groupId;

        public SharedTopic(String topic, String groupId) {
            this.topic = topic;
            this.groupId = groupId;
        }

        public String getTopic() {
            return topic;
        }

        public String getGroupId() {
            return groupId;
        }
    }
}
