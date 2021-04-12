package com.hivemq.configuration.entity.cluster;

import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateClientSessionEntity;
import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateOutgoingMessageFlowEntity;
import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateQueuedMessagesEntity;
import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateRetainedMessageEntity;
import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateSubscriptionsEntity;
import com.hivemq.configuration.entity.cluster.replicate.ClusterReplicateTopicTreeEntity;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

@XmlType(propOrder = {})
public class ClusterReplicatesEntity {
    @XmlElement(name = "retained-messages")
    private final ClusterReplicateRetainedMessageEntity retainedMessages = new ClusterReplicateRetainedMessageEntity();
    @XmlElement(name = "topic-tree")
    private final ClusterReplicateTopicTreeEntity topicTree = new ClusterReplicateTopicTreeEntity();
    @XmlElement(name = "client-session")
    private final ClusterReplicateClientSessionEntity clientSessions = new ClusterReplicateClientSessionEntity();
    @XmlElement(name = "subscriptions")
    private final ClusterReplicateSubscriptionsEntity subscriptions = new ClusterReplicateSubscriptionsEntity();
    @XmlElement(name = "queued-messages")
    private final ClusterReplicateQueuedMessagesEntity queuedMessages = new ClusterReplicateQueuedMessagesEntity();
    @XmlElement(name = "outgoing-message-flow")
    private final ClusterReplicateOutgoingMessageFlowEntity outgoingMessageFlow = new ClusterReplicateOutgoingMessageFlowEntity();

    public ClusterReplicateRetainedMessageEntity getRetainedMessages() {
        return this.retainedMessages;
    }

    public ClusterReplicateTopicTreeEntity getTopicTree() {
        return this.topicTree;
    }

    public ClusterReplicateClientSessionEntity getClientSessions() {
        return this.clientSessions;
    }

    public ClusterReplicateSubscriptionsEntity getSubscriptions() {
        return this.subscriptions;
    }

    public ClusterReplicateQueuedMessagesEntity getQueuedMessages() {
        return this.queuedMessages;
    }

    public ClusterReplicateOutgoingMessageFlowEntity getOutgoingMessageFlow() {
        return this.outgoingMessageFlow;
    }
}
