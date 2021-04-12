package by;

import bx.SubscriberWithQoS;
import cb1.ByteUtils;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Sets;
import com.google.common.collect.UnmodifiableIterator;
import com.google.common.util.concurrent.Striped;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import u.Filter;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

// TODO:
public class TopicTreeImpl implements TopicTree {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicTreeImpl.class);
    private final CopyOnWriteArrayList<SubscriberWithQoS> wildcardSubscribers = new CopyOnWriteArrayList<>();
    private final int nodeInitialCapacity;
    private final int subscriberBufferSize;
    private final Striped<ReadWriteLock> stripedLock;
    private final ConcurrentHashMap<String, TopicTreeNode> treeNodes = new ConcurrentHashMap<>();

    public TopicTreeImpl(int nodeInitialCapacity, int subscriberBufferSize) {
        this.nodeInitialCapacity = nodeInitialCapacity;
        this.subscriberBufferSize = subscriberBufferSize;
        this.stripedLock = Striped.lazyWeakReadWriteLock(64);
    }

    public void addSubscription(@NotNull String subscriber, @NotNull Topic topic, byte shared, @Nullable String groupId) {
        Preconditions.checkNotNull(subscriber, "Subscriber must not be null");
        Preconditions.checkNotNull(topic, "Topic must not be null");
        subscribe(subscriber, topic, shared, groupId);
    }


    private void subscribe(String subscriber, Topic topic, byte shared, @Nullable String groupId) {
        String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic.getTopic(), '/');
        if (segmentKeys.length > 1000) {
            LOGGER.warn("Subscription from {} on topic {} exceeds maximum segment count of 1000 segments, ignoring it",
                    subscriber, topic);
            return;
        }
        if (segmentKeys.length <= 0) {
            return;
        }
        SubscriberWithQoS subscriberWithQoS = new SubscriberWithQoS(subscriber, topic.getQoS().getQosNumber(), shared, groupId);
        if (segmentKeys.length == 1 && segmentKeys[0].equals("#")) {
            this.wildcardSubscribers.add(subscriberWithQoS);
            return;
        }
        String segmentKey = segmentKeys[0];
        Lock lock = this.stripedLock.get(segmentKey).writeLock();
        lock.lock();
        try {
            TopicTreeNode treeNode = this.treeNodes.get(segmentKey);
            if (treeNode == null) {
                treeNode = new TopicTreeNode(segmentKey, this.nodeInitialCapacity, this.subscriberBufferSize);
                this.treeNodes.put(segmentKey, treeNode);
            }
            if (segmentKeys.length == 1) {
                treeNode.addExcludeWildcard(subscriberWithQoS);
            } else {
                subscribe(subscriberWithQoS, segmentKeys, treeNode, 1);
            }
        } finally {
            lock.unlock();
        }
    }

    public Segment get(@NotNull String segmentKey) {
        Preconditions.checkNotNull(segmentKey, "Segment key must not be null");
        Lock lock = this.stripedLock.get(segmentKey).readLock();
        lock.lock();
        try {
            TopicTreeNode node = this.treeNodes.get(segmentKey);
            if (node != null) {
                return new Segment(segmentKey, getSegmentEntries(node, segmentKey));
            }
        } finally {
            lock.unlock();
        }
        return new Segment(segmentKey, ImmutableMap.of());
    }


    private ImmutableMap<String, Set<SubscriberWithQoS>> getSegmentEntries(TopicTreeNode node, String segmentKey) {
        ImmutableMap.Builder<String, Set<SubscriberWithQoS>> builder = ImmutableMap.builder();
        if (node.getExcludeWildcardSubscriberBuffer() != null) {
            Set<SubscriberWithQoS> subscribers = Sets.newHashSet(node.getExcludeWildcardSubscriberBuffer());
            subscribers.remove(null);
            builder.put(segmentKey, subscribers);
        }
        if (node.getWildcardSubscriberBuffer() != null) {
            Set<SubscriberWithQoS> subscribers = Sets.newHashSet(node.getWildcardSubscriberBuffer());
            subscribers.remove(null);
            builder.put(segmentKey, subscribers);
        }
        if (node.getChildNodes() != null) {
            for (int i = 0; i < node.getChildNodes().length; i++) {
                if (node.getChildNodes()[i] != null) {
                    String childSegmentKey = segmentKey + "/" + node.getChildNodes()[i].getSegment();
                    builder.putAll(getSegmentEntries(node.getChildNodes()[i], childSegmentKey));
                }
            }
        }
        return builder.build();
    }

    public ImmutableSet<Segment> get(@NotNull Filter filter) {
        ImmutableSet.Builder<Segment> builder = ImmutableSet.builder();
        this.treeNodes.values().stream()
                .forEach(node -> builder.add(get(node.getSegment())));
        return builder.build();
    }

    public void remove(@NotNull Filter filter) {
        this.treeNodes.keySet().stream()
                .filter(filter::test)
                .forEach(this.treeNodes::remove);
    }

    public CopyOnWriteArrayList<SubscriberWithQoS> getWildcardSubscribers() {
        return this.wildcardSubscribers;
    }

    public void merge(@NotNull Segment segment, @NotNull String segmentKey) {
        Preconditions.checkNotNull(segment, "Segment must not be null");
        Preconditions.checkNotNull(segmentKey, "Segment key must not be null");
        Lock lock = this.stripedLock.get(segmentKey).writeLock();
        lock.lock();
        try {
            ImmutableMap<String, Set<SubscriberWithQoS>> subscribers = get(segmentKey).getEntries();
            if (subscribers.isEmpty()) {
                segment.getEntries().entrySet().forEach(entry ->
                        entry.getValue().forEach(subscriber ->
                                addSubscription(subscriber.getSubscriber(),
                                        new Topic(entry.getKey(), QoS.valueOf(subscriber.getQosNumber())),
                                        subscriber.getShared(), subscriber.getGroupId())
                        )
                );
            } else {
                segment.getEntries().entrySet().forEach(entry -> {
                    if (subscribers.containsKey(entry.getKey())) {
                        entry.getValue().forEach(subscriber ->
                                addSubscription(subscriber.getSubscriber(),
                                        new Topic(entry.getKey(), QoS.valueOf(subscriber.getQosNumber())),
                                        ByteUtils.getTrueByte(subscriber.getShared(), 0),
                                        subscriber.getGroupId())
                        );
                    } else {
                        Set<SubscriberWithQoS> requestSubscribers = entry.getValue();
                        Set<SubscriberWithQoS> localSubscribers = entry.getValue();
                        Sets.SetView<SubscriberWithQoS> difference = Sets.difference(requestSubscribers, localSubscribers);
                        difference.forEach(subscriber -> {
                            removeSubscription(subscriber.getSubscriber(), entry.getKey());
                            addSubscription(subscriber.getSubscriber(),
                                    new Topic(entry.getKey(), QoS.valueOf(subscriber.getQosNumber())),
                                    ByteUtils.getTrueByte(subscriber.getShared(), 0), subscriber.getGroupId());
                        });
                    }
                });
            }
        } finally {
            lock.unlock();
        }
    }

    private void subscribe(SubscriberWithQoS subscriber, String[] segmentKeys, TopicTreeNode treeNode, int deep) {
        String segmentKey = segmentKeys[deep];
        if (segmentKey.equals("#")) {
            treeNode.addWildcard(subscriber);
            return;
        }
        TopicTreeNode childTreeNode = new TopicTreeNode(segmentKey, this.nodeInitialCapacity, this.subscriberBufferSize);
        childTreeNode = treeNode.addNode(childTreeNode);
        if (deep + 1 == segmentKeys.length) {
            childTreeNode.addExcludeWildcard(subscriber);
        } else {
            subscribe(subscriber, segmentKeys, childTreeNode, deep + 1);
        }
    }

    public ImmutableSet<SubscriberWithQoS> getSubscribers(@NotNull String topic) {
        return getSubscribers(topic, false);
    }

    public ImmutableSet<SubscriberWithQoS> getSubscribers(@NotNull String topic, boolean excludeWildcard) {
        Preconditions.checkNotNull(topic, "Topic must not be null");
        ImmutableSortedSet.Builder<SubscriberWithQoS> builder = ImmutableSortedSet.naturalOrder();
        if (!excludeWildcard) {
            builder.addAll(this.wildcardSubscribers);
        }
        if (this.treeNodes.isEmpty()) {
            return distinct(builder.build());
        }
        String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic, '/');
        String segmentKey = segmentKeys[0];
        Lock lock = this.stripedLock.get(segmentKey).readLock();
        lock.lock();
        try {
            TopicTreeNode node = this.treeNodes.get(segmentKey);
            if (node != null) {
                putSubscribers(node, builder, segmentKeys, 0);
            }
        } finally {
            lock.unlock();
        }
        if (!excludeWildcard) {
            lock = this.stripedLock.get("+").readLock();
            lock.lock();
            try {
                TopicTreeNode node = this.treeNodes.get("+");
                if (node != null) {
                    putSubscribers(node, builder, segmentKeys, 0);
                }
            } finally {
                lock.unlock();
            }
        }
        return distinct(builder.build());
    }

    private ImmutableSet<SubscriberWithQoS> distinct(ImmutableSortedSet<SubscriberWithQoS> subscribers) {
        if (subscribers.size() <= 1) {
            return subscribers;
        }
        ImmutableSortedSet.Builder<SubscriberWithQoS> builder = ImmutableSortedSet.naturalOrder();
        UnmodifiableIterator<SubscriberWithQoS> iterator = subscribers.iterator();
        SubscriberWithQoS current;
        for (SubscriberWithQoS previous = null; iterator.hasNext(); previous = current) {
            current = iterator.next();
            if (previous != null &&
                    !current.getSubscriber().equals(previous.getSubscriber())) {
                builder.add(previous);
            }
            if (!iterator.hasNext()) {
                builder.add(current);
            }
        }
        return builder.build();
    }

    private void putSubscribers(@NotNull TopicTreeNode parentNode, ImmutableSortedSet.Builder<SubscriberWithQoS> builder, String[] segmentKeys, int deep) {
        if (!segmentKeys[deep].equals(parentNode.getSegment()) &&
                !"+".equals(parentNode.getSegment())) {
            return;
        }
        if (parentNode.getWildcardSubscribers() != null) {
            builder.addAll(parentNode.getWildcardSubscribers().values());
        } else {
            SubscriberWithQoS[] wildcardSubscriberBuffer = parentNode.getWildcardSubscriberBuffer();
            if (wildcardSubscriberBuffer != null) {
                for (int index = 0; index < wildcardSubscriberBuffer.length; index++) {
                    SubscriberWithQoS subscriber = wildcardSubscriberBuffer[index];
                    if (subscriber != null) {
                        builder.add(subscriber);
                    }
                }
            }
        }
        boolean isLeaf = segmentKeys.length - 1 == deep;
        if (isLeaf) {
            if (TopicTreeUtils.excludeWildcardSubscriberSize(parentNode) > 0) {
                if (parentNode.getExcludeWildcardSubscribers() != null) {
                    builder.addAll(parentNode.getExcludeWildcardSubscribers().values());
                } else {
                    for (SubscriberWithQoS subscriber : parentNode.getExcludeWildcardSubscriberBuffer()) {
                        if (subscriber != null) {
                            builder.add(subscriber);
                        }
                    }
                }
            }
            return;
        }
        if (TopicTreeUtils.childNodeSize(parentNode) == 0) {
            return;
        }
        TopicTreeNode[] childNodes = parentNode.getChildNodes();
        if (parentNode.getChildNodeIndexes() != null) {
            TopicTreeNode childNode = getChildNode(segmentKeys[(deep + 1)], parentNode);
            TopicTreeNode singleLevelChildNode = getChildNode("+", parentNode);
            if (childNode != null) {
                putSubscribers(childNode, builder, segmentKeys, deep + 1);
            }
            if (singleLevelChildNode != null) {
                putSubscribers(singleLevelChildNode, builder, segmentKeys, deep + 1);
            }
            return;
        }
        for (TopicTreeNode node : childNodes) {
            if (node != null) {
                putSubscribers(node, builder, segmentKeys, deep + 1);
            }
        }
    }

    private TopicTreeNode getChildNode(@NotNull String segmentKey, @NotNull TopicTreeNode topicTreeNode) {
        Integer index = topicTreeNode.getChildNodeIndexes().get(segmentKey);
        if (index == null) {
            return null;
        }
        return topicTreeNode.getChildNodes()[index];
    }

    public void removeSubscriptions(@NotNull String subscriber) {
        Preconditions.checkNotNull(subscriber);
        removeWildcardSubscriber(subscriber);
        this.treeNodes.forEach((segmentKey, node) -> {
            Lock lock = this.stripedLock.get(segmentKey).writeLock();
            lock.lock();
            try {
                removeChildSubscriptions(node, subscriber);
                if (TopicTreeUtils.childNodeSize(node) == 0 &&
                        TopicTreeUtils.excludeWildcardSubscriberSize(node) == 0 &&
                        TopicTreeUtils.wildcardSubscriberSize(node) == 0) {
                    this.treeNodes.remove(node.getSegment());
                }
            } finally {
                lock.unlock();
            }
        });
    }

    private void removeWildcardSubscriber(@NotNull String subscriber) {
        ImmutableList.Builder<SubscriberWithQoS> builder = ImmutableList.builder();
        this.wildcardSubscribers.stream()
                .filter(subscriber::equals)
                .forEach(builder::add);
        this.wildcardSubscribers.removeAll(builder.build());
    }

    private boolean removeChildSubscriptions(@NotNull TopicTreeNode treeNode, @NotNull String subscriber) {
        treeNode.removeWildcard(subscriber);
        treeNode.removeExcludeWildcard(subscriber);
        TopicTreeNode[] childNodes = treeNode.getChildNodes();
        if (childNodes != null) {
            for (int i = 0; i < childNodes.length; i++) {
                TopicTreeNode childNode = childNodes[i];
                if (childNode != null) {
                    boolean bool = removeChildSubscriptions(childNode, subscriber);
                    if (bool) {
                        if (treeNode.getChildNodeIndexes() != null) {
                            treeNode.getChildNodeIndexes().remove(childNodes[i].getSegment());
                        }
                        childNodes[i] = null;
                    }
                }
            }
        }
        return a(treeNode);
    }

    public void removeSubscription(@NotNull String subscriber, @NotNull String topic) {
        Preconditions.checkNotNull(subscriber);
        Preconditions.checkNotNull(topic);
        if ("#".equals(topic)) {
            removeWildcardSubscriber(subscriber);
        }
        if (this.treeNodes.isEmpty()) {
            return;
        }
        String[] segmentKeys = StringUtils.splitPreserveAllTokens(topic, "/");
        TopicTreeNode[] treeNodes = new TopicTreeNode[segmentKeys.length];
        String segmentKey = segmentKeys[0];
        Lock lock = this.stripedLock.get(segmentKey).writeLock();
        lock.lock();
        try {
            TopicTreeNode treeNode = this.treeNodes.get(segmentKey);
            if (treeNode == null) {
                return;
            }
            if (segmentKeys.length == 1) {
                treeNode.removeExcludeWildcard(subscriber);
            }
            if (segmentKeys.length == 2 && segmentKeys[1].equals("#")) {
                treeNode.removeWildcard(subscriber);
            }
            a(treeNode, segmentKeys, treeNodes, 0);
            TopicTreeNode localc2 = a(treeNodes);
            if (localc2 != null) {
                String str2 = segmentKeys[(segmentKeys.length - 1)];
                if (str2.equals("#")) {
                    localc2.removeWildcard(subscriber);
                } else if (str2.equals(localc2.getSegment())) {
                    localc2.removeExcludeWildcard(subscriber);
                }
            }
            for (int i = treeNodes.length - 1; i > 0; i--) {
                TopicTreeNode localc3 = treeNodes[i];
                if ((localc3 != null) && (a(localc3))) {
                    TopicTreeNode localc4 = treeNodes[(i - 1)];
                    if (localc4 == null) {
                        localc4 = treeNode;
                    }
                    TopicTreeNode[] arrayOfc2 = localc4.getChildNodes();
                    for (int j = 0; j < arrayOfc2.length; j++) {
                        if (arrayOfc2[j] == localc3) {
                            if (localc4.getChildNodeIndexes() != null) {
                                localc4.getChildNodeIndexes().remove(arrayOfc2[j].getSegment());
                            }
                            arrayOfc2[j] = null;
                        }
                    }
                }
            }
            if ((TopicTreeUtils.childNodeSize(treeNode) == 0) &&
                    (TopicTreeUtils.excludeWildcardSubscriberSize(treeNode) == 0) &&
                    (TopicTreeUtils.wildcardSubscriberSize(treeNode) == 0)) {
                this.treeNodes.remove(treeNode.getSegment());
            }
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    private TopicTreeNode a(@NotNull TopicTreeNode[] paramArrayOfc) {
        for (int i = paramArrayOfc.length - 1; i >= 0; i--) {
            TopicTreeNode localc = paramArrayOfc[i];
            if (localc != null) {
                return localc;
            }
        }
        return null;
    }

    private void a(@NotNull TopicTreeNode paramc, @NotNull String[] paramArrayOfString, @NotNull TopicTreeNode[] paramArrayOfc, int paramInt) {
        Object localObject = null;
        if (paramc.getChildNodes() != null) {
            if (paramc.getChildNodeIndexes() != null) {
                Integer localInteger = (Integer) paramc.getChildNodeIndexes().get(paramArrayOfString[(paramInt + 1)]);
                if (localInteger == null) {
                    return;
                }
                localObject = paramc.getChildNodes()[localInteger];
            } else {
                for (int i = 0; i < paramc.getChildNodes().length; i++) {
                    TopicTreeNode localc = paramc.getChildNodes()[i];
                    if ((localc != null) && (paramInt + 2 <= paramArrayOfString.length) && (localc.getSegment().equals(paramArrayOfString[(paramInt + 1)]))) {
                        localObject = localc;
                        break;
                    }
                }
            }
            if (localObject != null) {
                paramArrayOfc[(paramInt + 1)] = localObject;
                a((TopicTreeNode) localObject, paramArrayOfString, paramArrayOfc, paramInt + 1);
            }
        }
    }

    private boolean a(@NotNull TopicTreeNode treeNode) {
        boolean bool1;
        if (treeNode.getExcludeWildcardSubscribers() != null) {
            bool1 = treeNode.getExcludeWildcardSubscribers().isEmpty();
        } else if (treeNode.getExcludeWildcardSubscriberBuffer() != null) {
            bool1 = a(treeNode.getExcludeWildcardSubscriberBuffer());
        } else {
            bool1 = true;
        }
        boolean bool2;
        if (treeNode.getWildcardSubscribers() != null) {
            bool2 = treeNode.getWildcardSubscribers().isEmpty();
        } else if (treeNode.getWildcardSubscriberBuffer() != null) {
            bool2 = a(treeNode.getWildcardSubscriberBuffer());
        } else {
            bool2 = true;
        }
        return (a(treeNode.getChildNodes())) && (bool2) && (bool1);
    }

    private boolean a(@Nullable Object[] paramArrayOfObject) {
        if (paramArrayOfObject == null) {
            return true;
        }
        for (int i = 0; i < paramArrayOfObject.length; i++) {
            if (paramArrayOfObject[i] != null) {
                return false;
            }
        }
        return true;
    }
}
