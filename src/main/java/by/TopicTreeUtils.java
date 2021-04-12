package by;

import bx.SubscriberWithQoS;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.Nullable;
import com.hivemq.spi.annotations.ReadOnly;

import java.util.Set;

public class TopicTreeUtils {

    public static int excludeWildcardSubscriberSize(@NotNull TopicTreeNode node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        if (node.getExcludeWildcardSubscribers() != null) {
            return node.getExcludeWildcardSubscribers().values().size();
        }
        SubscriberWithQoS[] subscribers = node.getExcludeWildcardSubscriberBuffer();
        return nonNullSize(subscribers);
    }

    @ReadOnly
    public static Set<SubscriberWithQoS> excludeWildcardSubscribers(@NotNull TopicTreeNode node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        ImmutableSet.Builder<SubscriberWithQoS> builder = new ImmutableSet.Builder<>();
        if (node.getExcludeWildcardSubscribers() != null) {
            builder.addAll(node.getExcludeWildcardSubscribers().values());
            return builder.build();
        }
        if (node.getExcludeWildcardSubscriberBuffer() == null) {
            return builder.build();
        }
        SubscriberWithQoS[] subscribers = node.getExcludeWildcardSubscriberBuffer();
        addAll(builder, subscribers);
        return builder.build();
    }

    public static int wildcardSubscriberSize(@NotNull TopicTreeNode node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        if (node.getWildcardSubscribers() != null) {
            return node.getWildcardSubscribers().values().size();
        }
        SubscriberWithQoS[] subscribers = node.getWildcardSubscriberBuffer();
        return nonNullSize(subscribers);
    }

    @ReadOnly
    public static Set<SubscriberWithQoS> wildcardSubscribers(@NotNull TopicTreeNode node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        ImmutableSet.Builder<SubscriberWithQoS> builder = new ImmutableSet.Builder<>();
        if (node.getWildcardSubscribers() != null) {
            builder.addAll(node.getWildcardSubscribers().values());
            return builder.build();
        }
        if (node.getWildcardSubscriberBuffer() == null) {
            return builder.build();
        }
        SubscriberWithQoS[] subscribers = node.getWildcardSubscriberBuffer();
        addAll(builder, subscribers);
        return builder.build();
    }

    public static int childNodeSize(@NotNull TopicTreeNode node) {
        Preconditions.checkNotNull(node, "Node must not be null");
        if (node.getChildNodeIndexes() != null) {
            return node.getChildNodeIndexes().size();
        }
        TopicTreeNode[] childNodes = node.getChildNodes();
        return nonNullSize(childNodes);
    }


    private static int nonNullSize(@Nullable Object[] objects) {
        if (objects == null) {
            return 0;
        }
        int count = 0;
        for (int index = 0; index < objects.length; index++) {
            if (objects[index] != null) {
                count++;
            }
        }
        return count;
    }

    private static void addAll(ImmutableSet.Builder<SubscriberWithQoS> builder, SubscriberWithQoS[] subscribers) {
        for (int i = 0; i < subscribers.length; i++) {
            SubscriberWithQoS subscriber = subscribers[i];
            if (subscriber != null) {
                builder.add(subscriber);
            }
        }
    }
}
