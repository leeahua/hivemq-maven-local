package by;

import bx.SubscriberWithQoS;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TopicTreeNode {
    private final String segment;
    private final int nodeInitialCapacity;
    private final int subscriberBufferSize;
    @Nullable
    private SubscriberWithQoS[] wildcardSubscriberBuffer;
    @Nullable
    private SubscriberWithQoS[] excludeWildcardSubscriberBuffer;
    @Nullable
    private TopicTreeNode[] childNodes;
    @Nullable
    private Map<String, Integer> childNodeIndexes;
    @Nullable
    private Multimap<String, SubscriberWithQoS> excludeWildcardSubscribers;
    @Nullable
    private Multimap<String, SubscriberWithQoS> wildcardSubscribers;

    public TopicTreeNode(String segment,
                         int nodeInitialCapacity,
                         int subscriberBufferSize) {
        this.segment = segment;
        this.nodeInitialCapacity = nodeInitialCapacity;
        this.subscriberBufferSize = subscriberBufferSize;
    }

    public TopicTreeNode addNode(TopicTreeNode childTreeNode) {
        if (this.childNodes != null) {
            if (this.childNodes.length > this.nodeInitialCapacity &&
                    this.childNodeIndexes == null) {
                this.childNodeIndexes = new HashMap<>();
                for (int index = 0; index < this.childNodes.length; index++) {
                    this.childNodeIndexes.put(this.childNodes[index].getSegment(), index);
                }
            }
            if (this.childNodeIndexes != null) {
                Integer index = this.childNodeIndexes.get(childTreeNode.getSegment());
                if (index != null) {
                    return this.childNodes[index];
                }
            } else {
                for (int index = 0; index < this.childNodes.length; index++) {
                    TopicTreeNode childNode = this.childNodes[index];
                    if (childNode != null &&
                            childNode.getSegment().equals(childTreeNode.getSegment())) {
                        return childNode;
                    }
                }
            }
        } else {
            this.childNodes = new TopicTreeNode[0];
        }
        Integer emptyNodeIndex = nextEmptyNodeIndex(this.childNodes);
        if (emptyNodeIndex != null) {
            this.childNodes[emptyNodeIndex] = childTreeNode;
            if (this.childNodeIndexes != null) {
                this.childNodeIndexes.put(childTreeNode.getSegment(), emptyNodeIndex);
            }
        } else {
            TopicTreeNode[] newChildNodeStack = new TopicTreeNode[this.childNodes.length + 1];
            System.arraycopy(this.childNodes, 0, newChildNodeStack, 0, this.childNodes.length);
            newChildNodeStack[(newChildNodeStack.length - 1)] = childTreeNode;
            this.childNodes = newChildNodeStack;
            if (this.childNodeIndexes != null) {
                this.childNodeIndexes.put(childTreeNode.getSegment(), newChildNodeStack.length - 1);
            }
        }
        return childTreeNode;
    }

    @Nullable
    private Integer nextEmptyNodeIndex(TopicTreeNode[] nodeStack) {
        for (int index = 0; index < nodeStack.length; index++) {
            if (nodeStack[index] == null) {
                return index;
            }
        }
        return null;
    }

    public void addExcludeWildcard(SubscriberWithQoS subscriber) {
        if (this.excludeWildcardSubscribers == null &&
                TopicTreeUtils.excludeWildcardSubscriberSize(this) > this.subscriberBufferSize) {
            this.excludeWildcardSubscribers = HashMultimap.create();
            if (this.excludeWildcardSubscriberBuffer != null) {
                for (SubscriberWithQoS s : this.excludeWildcardSubscriberBuffer) {
                    this.excludeWildcardSubscribers.put(s.getSubscriber(), s);
                }
                this.excludeWildcardSubscriberBuffer = null;
            }
        }
        if (this.excludeWildcardSubscribers != null) {
            this.excludeWildcardSubscribers.put(subscriber.getSubscriber(), subscriber);
        } else if (this.excludeWildcardSubscriberBuffer == null) {
            this.excludeWildcardSubscriberBuffer = new SubscriberWithQoS[]{subscriber};
        } else {
            this.excludeWildcardSubscriberBuffer = add(subscriber, this.excludeWildcardSubscriberBuffer);
        }
    }

    public void removeExcludeWildcard(@NotNull String subscriber) {
        if (this.excludeWildcardSubscribers != null) {
            this.excludeWildcardSubscribers.removeAll(subscriber);
        } else if (this.excludeWildcardSubscriberBuffer != null) {
            remove(this.excludeWildcardSubscriberBuffer, subscriber);
        }
    }

    public void addWildcard(SubscriberWithQoS subscriber) {
        if (this.wildcardSubscribers == null &&
                TopicTreeUtils.wildcardSubscriberSize(this) > this.subscriberBufferSize) {
            this.wildcardSubscribers = HashMultimap.create();
            if (this.wildcardSubscriberBuffer != null) {
                for (SubscriberWithQoS s : this.wildcardSubscriberBuffer) {
                    this.wildcardSubscribers.put(s.getSubscriber(), s);
                }
                this.wildcardSubscriberBuffer = null;
            }
        }
        if (this.wildcardSubscribers != null) {
            this.wildcardSubscribers.put(subscriber.getSubscriber(), subscriber);
        } else if (this.wildcardSubscriberBuffer == null) {
            this.wildcardSubscriberBuffer = new SubscriberWithQoS[]{subscriber};
        } else {
            this.wildcardSubscriberBuffer = add(subscriber, this.wildcardSubscriberBuffer);
        }
    }

    public void removeWildcard(@NotNull String subscriber) {
        if (this.wildcardSubscribers != null) {
            this.wildcardSubscribers.removeAll(subscriber);
        } else if (this.wildcardSubscriberBuffer != null) {
            remove(this.wildcardSubscriberBuffer, subscriber);
        }
    }

    private SubscriberWithQoS[] add(SubscriberWithQoS subscriber,
                                    @NotNull SubscriberWithQoS[] subscribers) {
        if (addIfNull(subscribers, subscriber)) {
            return subscribers;
        }
        SubscriberWithQoS[] newSubscribers = new SubscriberWithQoS[subscribers.length + 1];
        System.arraycopy(subscribers, 0, newSubscribers, 0, subscribers.length);
        newSubscribers[subscribers.length] = subscriber;
        return newSubscribers;
    }

    private boolean addIfNull(@NotNull SubscriberWithQoS[] subscribers, SubscriberWithQoS subscriber) {
        for (int index = 0; index < subscribers.length; index++) {
            if (subscribers[index] == null) {
                subscribers[index] = subscriber;
                return true;
            }
            if (subscriber.equals(subscribers[index])) {
                return true;
            }
        }
        return false;
    }

    private void remove(@NotNull SubscriberWithQoS[] subscribers, @NotNull String subscriber) {
        for (int index = 0; index < subscribers.length; index++) {
            SubscriberWithQoS subscriberWithQoS = subscribers[index];
            if (subscriberWithQoS != null &&
                    subscriber.equals(subscriberWithQoS.getSubscriber())) {
                subscribers[index] = null;
            }
        }
    }

    @Nullable
    public TopicTreeNode[] getChildNodes() {
        return childNodes;
    }

    @Nullable
    public SubscriberWithQoS[] getExcludeWildcardSubscriberBuffer() {
        return excludeWildcardSubscriberBuffer;
    }

    @Nullable
    public SubscriberWithQoS[] getWildcardSubscriberBuffer() {
        return wildcardSubscriberBuffer;
    }

    @Nullable
    public Multimap<String, SubscriberWithQoS> getExcludeWildcardSubscribers() {
        return excludeWildcardSubscribers;
    }

    @Nullable
    public Multimap<String, SubscriberWithQoS> getWildcardSubscribers() {
        return wildcardSubscribers;
    }

    public String getSegment() {
        return this.segment;
    }

    public Map<String, Integer> getChildNodeIndexes() {
        return childNodeIndexes;
    }

    public void print(String value, boolean last) {
        String currentValue = this.segment + " w: " + Arrays.toString(this.wildcardSubscriberBuffer) + " e: " + Arrays.toString(this.excludeWildcardSubscriberBuffer);
        System.out.println(value + (last ? "└──  " : "├──  ") + currentValue);
        TopicTreeNode lastChild = null;
        if (this.childNodes != null) {
            for (int index = 0; index < this.childNodes.length; index++) {
                if (index != this.childNodes.length - 1) {
                    TopicTreeNode child = this.childNodes[index];
                    child.print(value + (last ? "    " : "│   "), false);
                } else {
                    lastChild = this.childNodes[index];
                }
            }
            if (lastChild != null) {
                lastChild.print(value + (last ? "    " : "│   "), true);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TopicTreeNode that = (TopicTreeNode) o;
        return Objects.equals(segment, that.segment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(segment);
    }

    @Override
    public String toString() {
        return "TopicTreeNode{" +
                "segment='" + segment + '\'' +
                '}';
    }
}
