package k;

import n.ClusterReplicateClientSession;
import n.ClusterReplicateOutgoingMessageFlow;
import n.ClusterReplicateQueuedMessages;
import n.ClusterReplicateRetainedMessage;
import n.ClusterReplicateSubscriptions;
import n.ClusterReplicateTopicTree;

public class ClusterReplicates {
    private final ClusterReplicateClientSession clientSession;
    private final ClusterReplicateQueuedMessages queuedMessages;
    private final ClusterReplicateOutgoingMessageFlow outgoingMessageFlow;
    private final ClusterReplicateRetainedMessage retainedMessage;
    private final ClusterReplicateSubscriptions subscriptions;
    private final ClusterReplicateTopicTree topicTree;

    public ClusterReplicates(ClusterReplicateClientSession clientSession,
                             ClusterReplicateQueuedMessages queuedMessages,
                             ClusterReplicateOutgoingMessageFlow outgoingMessageFlow,
                             ClusterReplicateRetainedMessage retainedMessage,
                             ClusterReplicateSubscriptions subscriptions,
                             ClusterReplicateTopicTree topicTree) {
        this.clientSession = clientSession;
        this.queuedMessages = queuedMessages;
        this.outgoingMessageFlow = outgoingMessageFlow;
        this.retainedMessage = retainedMessage;
        this.subscriptions = subscriptions;
        this.topicTree = topicTree;
    }

    public ClusterReplicateClientSession getClientSession() {
        return clientSession;
    }

    public ClusterReplicateQueuedMessages getQueuedMessages() {
        return queuedMessages;
    }

    public ClusterReplicateOutgoingMessageFlow getOutgoingMessageFlow() {
        return outgoingMessageFlow;
    }

    public ClusterReplicateRetainedMessage getRetainedMessage() {
        return retainedMessage;
    }

    public ClusterReplicateSubscriptions getSubscriptions() {
        return subscriptions;
    }

    public ClusterReplicateTopicTree getTopicTree() {
        return topicTree;
    }
}
