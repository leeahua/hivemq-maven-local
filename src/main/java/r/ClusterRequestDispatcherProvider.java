package r;

import a1.ClusterReceiver;
import a1.ClusterRequestDispatcher;
import a1.ClusterRequestDispatcherImpl;
import aa.TopicTreeAddRequest;
import aa.TopicTreeGetRequest;
import aa.TopicTreeRemoveReplicateRequest;
import aa.TopicTreeRemoveRequest;
import aa.TopicTreeReplicateAddRequest;
import aa.TopicTreeReplicateSegmentRequest;
import b1.AllClientsRequestReceiver;
import b1.ClientConnectedRequestReceiver;
import b1.ClientDataRequestReceiver;
import b1.ClientDisconnectedRequestReceiver;
import b1.ClientSessionGetRequestReceiver;
import b1.ClientSessionPutAllRequestReceiver;
import b1.ClientSessionReplicateRequestReceiver;
import b1.ClientTakeoverRequestReceiver;
import b1.ConnectedClientsRequestReceiver;
import b1.DisconnectedClientsRequestReceiver;
import b1.NodeForPublishRequestReceiver;
import c1.OutgoingMessageFlowPutReplicateRequestReceiver;
import c1.OutgoingMessageFlowPutRequestReceiver;
import c1.OutgoingMessageFlowRemoveAllReplicateRequestReceiver;
import c1.OutgoingMessageFlowReplicateRequestReceiver;
import com.google.common.util.concurrent.ListeningExecutorService;
import d.CacheScoped;
import d1.MessageQueueDrainedAckRequestReceiver;
import d1.MessageQueueOfferReplicateRequestReceiver;
import d1.MessageQueuePollRequestReceiver;
import d1.MessageQueueRemoveAllReplicateRequestReceiver;
import d1.MessageQueueRemoveReplicateRequestReceiver;
import d1.MessageQueueReplicateRequestReceiver;
import e1.ClusterPubRelRequestReceiver;
import e1.ClusterPublishRequestReceiver;
import f1.RetainedMessageAddReplicateRequestReceiver;
import f1.RetainedMessageAddRequestReceiver;
import f1.RetainedMessageClearRequestReceiver;
import f1.RetainedMessageGetRequestReceiver;
import f1.RetainedMessagePutAllRequestReceiver;
import f1.RetainedMessageRemoveReplicateRequestReceiver;
import f1.RetainedMessageRemoveRequestReceiver;
import g1.AllClusterStatesRequestReceiver;
import g1.ClusterContextRequestReceiver;
import g1.ClusterStateRequestReceiver;
import g1.LicenseCheckRequestReceiver;
import g1.LicenseInformationRequestReceiver;
import g1.MetricRequestReceiver;
import g1.NodeInformationRequestReceiver;
import g1.NotificationRequestReceiver;
import g1.ReplicateFinishedRequestReceiver;
import h1.ClientSessionSubscriptionAddReplicateRequestReceiver;
import h1.ClientSessionSubscriptionAddRequestReceiver;
import h1.ClientSessionSubscriptionAllReplicateRequestReceiver;
import h1.ClientSessionSubscriptionGetRequestReceiver;
import h1.ClientSessionSubscriptionRemoveAllReplicateRequestReceiver;
import h1.ClientSessionSubscriptionRemoveReplicateRequestReceiver;
import h1.ClientSessionSubscriptionRemoveRequestReceiver;
import i1.TopicTreeAddRequestReceiver;
import i1.TopicTreeGetRequestReceiver;
import i1.TopicTreeRemoveReplicateRequestReceiver;
import i1.TopicTreeRemoveRequestReceiver;
import i1.TopicTreeReplicateAddRequestReceiver;
import i1.TopicTreeReplicateSegmentRequestReceiver;
import j1.ClusterRequest;
import s.Rpc;
import t1.AllClientsRequest;
import t1.ClientConnectedRequest;
import t1.ClientDataRequest;
import t1.ClientDisconnectedRequest;
import t1.ClientSessionGetRequest;
import t1.ClientSessionPutAllRequest;
import t1.ClientSessionReplicateRequest;
import t1.ClientTakeoverRequest;
import t1.ConnectedClientsRequest;
import t1.DisconnectedClientsRequest;
import t1.NodeForPublishRequest;
import u1.OutgoingMessageFlowPutReplicateRequest;
import u1.OutgoingMessageFlowPutRequest;
import u1.OutgoingMessageFlowRemoveAllReplicateRequest;
import u1.OutgoingMessageFlowReplicateRequest;
import v1.MessageQueueDrainedAckRequest;
import v1.MessageQueueOfferReplicateRequest;
import v1.MessageQueuePollRequest;
import v1.MessageQueueRemoveAllReplicateRequest;
import v1.MessageQueueRemoveReplicateRequest;
import v1.MessageQueueReplicateRequest;
import w1.ClusterPubRelRequest;
import w1.ClusterPublishRequest;
import x1.RetainedMessageAddReplicateRequest;
import x1.RetainedMessageAddRequest;
import x1.RetainedMessageClearRequest;
import x1.RetainedMessageGetRequest;
import x1.RetainedMessagePutAllRequest;
import x1.RetainedMessageRemoveReplicateRequest;
import x1.RetainedMessageRemoveRequest;
import y1.AllClusterStatesRequest;
import y1.ClusterContextRequest;
import y1.ClusterStateNotificationRequest;
import y1.ClusterStateRequest;
import y1.LicenseCheckRequest;
import y1.LicenseInformationRequest;
import y1.MetricRequest;
import y1.NodeInformationRequest;
import y1.ReplicateFinishedRequest;
import z1.ClientSessionSubscriptionAddReplicateRequest;
import z1.ClientSessionSubscriptionAddRequest;
import z1.ClientSessionSubscriptionAllReplicateRequest;
import z1.ClientSessionSubscriptionGetRequest;
import z1.ClientSessionSubscriptionRemoveAllReplicateRequest;
import z1.ClientSessionSubscriptionRemoveReplicateRequest;
import z1.ClientSessionSubscriptionRemoveRequest;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@CacheScoped
public class ClusterRequestDispatcherProvider implements Provider<ClusterRequestDispatcher> {
    private final Provider<ListeningExecutorService> rpcExecutorProvider;
    private final Provider<RetainedMessageGetRequestReceiver> retainedMessageGetRequestReceiverProvider;
    private final Provider<RetainedMessageAddRequestReceiver> retainedMessageAddRequestReceiverProvider;
    private final Provider<RetainedMessageRemoveRequestReceiver> retainedMessageRemoveRequestReceiverProvider;
    private final Provider<RetainedMessageAddReplicateRequestReceiver> retainedMessageReplicateAddRequestReceiverProvider;
    private final Provider<RetainedMessageRemoveReplicateRequestReceiver> retainedMessageReplicateRemoveRequestReceiverProvider;
    private final Provider<RetainedMessagePutAllRequestReceiver> retainedMessagePutAllRequestReceiverProvider;
    private final Provider<RetainedMessageClearRequestReceiver> retainedMessageClearRequestReceiverProvider;
    private final Provider<ClusterStateRequestReceiver> stateRequestReceiverProvider;
    private final Provider<NotificationRequestReceiver> notificationRequestReceiverProvider;
    private final Provider<ReplicateFinishedRequestReceiver> replicationFinishedRequestReceiverProvider;
    private final Provider<ClusterContextRequestReceiver> clusterContextRequestReceiverProvider;
    private final Provider<NodeInformationRequestReceiver> clusterVersionRequestReceiverProvider;
    private final Provider<MetricRequestReceiver> metricRequestReceiverProvider;
    private final Provider<AllClusterStatesRequestReceiver> allClusterStatesRequestReceiverProvider;
    private final Provider<LicenseCheckRequestReceiver> licenseCheckRequestReceiverProvider;
    private final Provider<LicenseInformationRequestReceiver> licenseInformationRequestReceiverProvider;
    private final Provider<TopicTreeAddRequestReceiver> topicTreeAddRequestReceiverProvider;
    private final Provider<TopicTreeRemoveRequestReceiver> topicTreeRemoveRequestReceiverProvider;
    private final Provider<TopicTreeReplicateAddRequestReceiver> topicTreeReplicateAddRequestReceiverProvider;
    private final Provider<TopicTreeRemoveReplicateRequestReceiver> topicTreeReplicateRemoveRequestReceiverProvider;
    private final Provider<TopicTreeGetRequestReceiver> topicTreeGetRequestReceiverProvider;
    private final Provider<TopicTreeReplicateSegmentRequestReceiver> topicTreeReplicateSegmentsRequestReceiverProvider;
    private final Provider<ClientSessionGetRequestReceiver> clientSessionGetRequestReceiverProvider;
    private final Provider<ClientConnectedRequestReceiver> clientConnectedRequestReceiverProvider;
    private final Provider<ClientDisconnectedRequestReceiver> clientDisconnectedRequestReceiverProvider;
    private final Provider<ClientSessionPutAllRequestReceiver> clientSessionPutAllRequestReceiverProvider;
    private final Provider<ClientSessionReplicateRequestReceiver> clientSessionReplicationRequestReceiverProvider;
    private final Provider<ClientTakeoverRequestReceiver> clientTakeoverRequestReceiverProvider;
    private final Provider<ConnectedClientsRequestReceiver> connectedClientsRequestReceiverProvider;
    private final Provider<DisconnectedClientsRequestReceiver> disconnectedClientsRequestReceiverProvider;
    private final Provider<ClientDataRequestReceiver> connectedClientRequestReceiverProvider;
    private final Provider<AllClientsRequestReceiver> allClientsRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionGetRequestReceiver> clientSessionSubscriptionGetRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionAddRequestReceiver> clientSessionSubscriptionAddRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionRemoveRequestReceiver> clientSessionSubscriptionRemoveRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionAddReplicateRequestReceiver> clientSessionSubscriptionReplicateAddRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionRemoveReplicateRequestReceiver> clientSessionSubscriptionReplicateRemoveRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionRemoveAllReplicateRequestReceiver> clientSessionSubscriptionReplicateRemoveAllRequestReceiverProvider;
    private final Provider<ClientSessionSubscriptionAllReplicateRequestReceiver> clientSessionSubscriptionReplicateAllRequestReceiverProvider;
    private final Provider<NodeForPublishRequestReceiver> nodeForPublishRequestReceiverProvider;
    private final Provider<ClusterPublishRequestReceiver> clusterPublishRequestReceiverProvider;
    private final Provider<ClusterPubRelRequestReceiver> clusterPubRelRequestReceiverProvider;
    private final Provider<MessageQueueOfferReplicateRequestReceiver> messageQueueOfferReplicationRequestReceiverProvider;
    private final Provider<MessageQueuePollRequestReceiver> messageQueuePollRequestReceiverProvider;
    private final Provider<MessageQueueRemoveReplicateRequestReceiver> messageQueueRemoveReplicationRequestReceiverProvider;
    private final Provider<MessageQueueDrainedAckRequestReceiver> messageQueueDrainedAckRequestReceiverProvider;
    private final Provider<MessageQueueReplicateRequestReceiver> messageQueueReplicationRequestReceiverProvider;
    private final Provider<MessageQueueRemoveAllReplicateRequestReceiver> messageQueueRemoveAllReplicateRequestReceiverProvider;
    private final Provider<OutgoingMessageFlowPutRequestReceiver> outgoingMessageFlowPutRequestReceiverProvider;
    private final Provider<OutgoingMessageFlowPutReplicateRequestReceiver> outgoingMessageFlowPutReplicateRequestReceiverProvider;
    private final Provider<OutgoingMessageFlowRemoveAllReplicateRequestReceiver> outgoingMessageFlowRemoveAllReplicationRequestReceiverProvider;
    private final Provider<OutgoingMessageFlowReplicateRequestReceiver> outgoingMessageFlowReplicationRequestReceiverProvider;

    @Inject
    public ClusterRequestDispatcherProvider(
            @Rpc Provider<ListeningExecutorService> rpcExecutorProvider,
            Provider<RetainedMessageGetRequestReceiver> retainedMessageGetRequestReceiverProvider,
            Provider<RetainedMessageAddRequestReceiver> retainedMessageAddRequestReceiverProvider,
            Provider<RetainedMessageRemoveRequestReceiver> retainedMessageRemoveRequestReceiverProvider,
            Provider<RetainedMessageAddReplicateRequestReceiver> retainedMessageReplicateAddRequestReceiverProvider,
            Provider<RetainedMessageRemoveReplicateRequestReceiver> retainedMessageReplicateRemoveRequestReceiverProvider,
            Provider<RetainedMessagePutAllRequestReceiver> retainedMessagePutAllRequestReceiverProvider,
            Provider<RetainedMessageClearRequestReceiver> retainedMessageClearRequestReceiverProvider,
            Provider<ClusterStateRequestReceiver> stateRequestReceiverProvider,
            Provider<NotificationRequestReceiver> notificationRequestReceiverProvider,
            Provider<ReplicateFinishedRequestReceiver> replicationFinishedRequestReceiverProvider,
            Provider<ClusterContextRequestReceiver> clusterContextRequestReceiverProvider,
            Provider<NodeInformationRequestReceiver> clusterVersionRequestReceiverProvider,
            Provider<MetricRequestReceiver> metricRequestReceiverProvider,
            Provider<AllClusterStatesRequestReceiver> allClusterStatesRequestReceiverProvider,
            Provider<LicenseCheckRequestReceiver> licenseCheckRequestReceiverProvider,
            Provider<LicenseInformationRequestReceiver> licenseInformationRequestReceiverProvider,
            Provider<TopicTreeAddRequestReceiver> topicTreeAddRequestReceiverProvider,
            Provider<TopicTreeRemoveRequestReceiver> topicTreeRemoveRequestReceiverProvider,
            Provider<TopicTreeReplicateAddRequestReceiver> topicTreeReplicateAddRequestReceiverProvider,
            Provider<TopicTreeRemoveReplicateRequestReceiver> topicTreeReplicateRemoveRequestReceiverProvider,
            Provider<TopicTreeGetRequestReceiver> topicTreeGetRequestReceiverProvider,
            Provider<TopicTreeReplicateSegmentRequestReceiver> topicTreeReplicateSegmentsRequestReceiverProvider,
            Provider<ClientSessionGetRequestReceiver> clientSessionGetRequestReceiverProvider,
            Provider<ClientConnectedRequestReceiver> clientConnectedRequestReceiverProvider,
            Provider<ClientDisconnectedRequestReceiver> clientDisconnectedRequestReceiverProvider,
            Provider<ClientSessionPutAllRequestReceiver> clientSessionPutAllRequestReceiverProvider,
            Provider<ClientSessionReplicateRequestReceiver> clientSessionReplicationRequestReceiverProvider,
            Provider<ClientTakeoverRequestReceiver> clientTakeoverRequestReceiverProvider,
            Provider<ConnectedClientsRequestReceiver> connectedClientsRequestReceiverProvider,
            Provider<DisconnectedClientsRequestReceiver> disconnectedClientsRequestReceiverProvider,
            Provider<ClientDataRequestReceiver> connectedClientRequestReceiverProvider,
            Provider<AllClientsRequestReceiver> allClientsRequestReceiverProvider,
            Provider<ClientSessionSubscriptionGetRequestReceiver> clientSessionSubscriptionGetRequestReceiverProvider,
            Provider<ClientSessionSubscriptionAddRequestReceiver> clientSessionSubscriptionAddRequestReceiverProvider,
            Provider<ClientSessionSubscriptionRemoveRequestReceiver> clientSessionSubscriptionRemoveRequestReceiverProvider,
            Provider<ClientSessionSubscriptionAddReplicateRequestReceiver> clientSessionSubscriptionReplicateAddRequestReceiverProvider,
            Provider<ClientSessionSubscriptionRemoveReplicateRequestReceiver> clientSessionSubscriptionReplicateRemoveRequestReceiverProvider,
            Provider<ClientSessionSubscriptionRemoveAllReplicateRequestReceiver> clientSessionSubscriptionReplicateRemoveAllRequestReceiverProvider,
            Provider<ClientSessionSubscriptionAllReplicateRequestReceiver> clientSessionSubscriptionReplicateAllRequestReceiverProvider,
            Provider<NodeForPublishRequestReceiver> nodeForPublishRequestReceiverProvider,
            Provider<ClusterPublishRequestReceiver> clusterPublishRequestReceiverProvider,
            Provider<ClusterPubRelRequestReceiver> clusterPubRelRequestReceiverProvider,
            Provider<MessageQueueOfferReplicateRequestReceiver> messageQueueOfferReplicationRequestReceiverProvider,
            Provider<MessageQueuePollRequestReceiver> messageQueuePollRequestReceiverProvider,
            Provider<MessageQueueRemoveReplicateRequestReceiver> messageQueueRemoveReplicationRequestReceiverProvider,
            Provider<MessageQueueDrainedAckRequestReceiver> messageQueueDrainedAckRequestReceiverProvider,
            Provider<MessageQueueReplicateRequestReceiver> messageQueueReplicationRequestReceiverProvider,
            Provider<MessageQueueRemoveAllReplicateRequestReceiver> messageQueueRemoveAllReplicateRequestReceiverProvider,
            Provider<OutgoingMessageFlowPutRequestReceiver> outgoingMessageFlowPutRequestReceiverProvider,
            Provider<OutgoingMessageFlowPutReplicateRequestReceiver> outgoingMessageFlowPutReplicateRequestReceiverProvider,
            Provider<OutgoingMessageFlowRemoveAllReplicateRequestReceiver> outgoingMessageFlowRemoveAllReplicationRequestReceiverProvider,
            Provider<OutgoingMessageFlowReplicateRequestReceiver> outgoingMessageFlowReplicationRequestReceiverProvider) {
        this.rpcExecutorProvider = rpcExecutorProvider;
        this.retainedMessageGetRequestReceiverProvider = retainedMessageGetRequestReceiverProvider;
        this.retainedMessageAddRequestReceiverProvider = retainedMessageAddRequestReceiverProvider;
        this.retainedMessageRemoveRequestReceiverProvider = retainedMessageRemoveRequestReceiverProvider;
        this.retainedMessageReplicateAddRequestReceiverProvider = retainedMessageReplicateAddRequestReceiverProvider;
        this.retainedMessageReplicateRemoveRequestReceiverProvider = retainedMessageReplicateRemoveRequestReceiverProvider;
        this.retainedMessagePutAllRequestReceiverProvider = retainedMessagePutAllRequestReceiverProvider;
        this.retainedMessageClearRequestReceiverProvider = retainedMessageClearRequestReceiverProvider;
        this.stateRequestReceiverProvider = stateRequestReceiverProvider;
        this.notificationRequestReceiverProvider = notificationRequestReceiverProvider;
        this.replicationFinishedRequestReceiverProvider = replicationFinishedRequestReceiverProvider;
        this.clusterContextRequestReceiverProvider = clusterContextRequestReceiverProvider;
        this.clusterVersionRequestReceiverProvider = clusterVersionRequestReceiverProvider;
        this.metricRequestReceiverProvider = metricRequestReceiverProvider;
        this.allClusterStatesRequestReceiverProvider = allClusterStatesRequestReceiverProvider;
        this.licenseCheckRequestReceiverProvider = licenseCheckRequestReceiverProvider;
        this.licenseInformationRequestReceiverProvider = licenseInformationRequestReceiverProvider;
        this.topicTreeAddRequestReceiverProvider = topicTreeAddRequestReceiverProvider;
        this.topicTreeRemoveRequestReceiverProvider = topicTreeRemoveRequestReceiverProvider;
        this.topicTreeReplicateAddRequestReceiverProvider = topicTreeReplicateAddRequestReceiverProvider;
        this.topicTreeReplicateRemoveRequestReceiverProvider = topicTreeReplicateRemoveRequestReceiverProvider;
        this.topicTreeGetRequestReceiverProvider = topicTreeGetRequestReceiverProvider;
        this.topicTreeReplicateSegmentsRequestReceiverProvider = topicTreeReplicateSegmentsRequestReceiverProvider;
        this.clientSessionGetRequestReceiverProvider = clientSessionGetRequestReceiverProvider;
        this.clientConnectedRequestReceiverProvider = clientConnectedRequestReceiverProvider;
        this.clientDisconnectedRequestReceiverProvider = clientDisconnectedRequestReceiverProvider;
        this.clientSessionPutAllRequestReceiverProvider = clientSessionPutAllRequestReceiverProvider;
        this.clientSessionReplicationRequestReceiverProvider = clientSessionReplicationRequestReceiverProvider;
        this.clientTakeoverRequestReceiverProvider = clientTakeoverRequestReceiverProvider;
        this.connectedClientsRequestReceiverProvider = connectedClientsRequestReceiverProvider;
        this.disconnectedClientsRequestReceiverProvider = disconnectedClientsRequestReceiverProvider;
        this.connectedClientRequestReceiverProvider = connectedClientRequestReceiverProvider;
        this.allClientsRequestReceiverProvider = allClientsRequestReceiverProvider;
        this.clientSessionSubscriptionGetRequestReceiverProvider = clientSessionSubscriptionGetRequestReceiverProvider;
        this.clientSessionSubscriptionAddRequestReceiverProvider = clientSessionSubscriptionAddRequestReceiverProvider;
        this.clientSessionSubscriptionRemoveRequestReceiverProvider = clientSessionSubscriptionRemoveRequestReceiverProvider;
        this.clientSessionSubscriptionReplicateAddRequestReceiverProvider = clientSessionSubscriptionReplicateAddRequestReceiverProvider;
        this.clientSessionSubscriptionReplicateRemoveRequestReceiverProvider = clientSessionSubscriptionReplicateRemoveRequestReceiverProvider;
        this.clientSessionSubscriptionReplicateRemoveAllRequestReceiverProvider = clientSessionSubscriptionReplicateRemoveAllRequestReceiverProvider;
        this.clientSessionSubscriptionReplicateAllRequestReceiverProvider = clientSessionSubscriptionReplicateAllRequestReceiverProvider;
        this.nodeForPublishRequestReceiverProvider = nodeForPublishRequestReceiverProvider;
        this.clusterPublishRequestReceiverProvider = clusterPublishRequestReceiverProvider;
        this.clusterPubRelRequestReceiverProvider = clusterPubRelRequestReceiverProvider;
        this.messageQueueOfferReplicationRequestReceiverProvider = messageQueueOfferReplicationRequestReceiverProvider;
        this.messageQueuePollRequestReceiverProvider = messageQueuePollRequestReceiverProvider;
        this.messageQueueRemoveReplicationRequestReceiverProvider = messageQueueRemoveReplicationRequestReceiverProvider;
        this.messageQueueDrainedAckRequestReceiverProvider = messageQueueDrainedAckRequestReceiverProvider;
        this.messageQueueReplicationRequestReceiverProvider = messageQueueReplicationRequestReceiverProvider;
        this.messageQueueRemoveAllReplicateRequestReceiverProvider = messageQueueRemoveAllReplicateRequestReceiverProvider;
        this.outgoingMessageFlowPutRequestReceiverProvider = outgoingMessageFlowPutRequestReceiverProvider;
        this.outgoingMessageFlowPutReplicateRequestReceiverProvider = outgoingMessageFlowPutReplicateRequestReceiverProvider;
        this.outgoingMessageFlowRemoveAllReplicationRequestReceiverProvider = outgoingMessageFlowRemoveAllReplicationRequestReceiverProvider;
        this.outgoingMessageFlowReplicationRequestReceiverProvider = outgoingMessageFlowReplicationRequestReceiverProvider;
    }

    @Override
    public ClusterRequestDispatcher get() {
        Map<Class<? extends ClusterRequest>, ClusterReceiver<?>> receivers = new ConcurrentHashMap();
        receivers.put(RetainedMessageGetRequest.class, this.retainedMessageGetRequestReceiverProvider.get());
        receivers.put(RetainedMessageAddRequest.class, this.retainedMessageAddRequestReceiverProvider.get());
        receivers.put(RetainedMessageRemoveRequest.class, this.retainedMessageRemoveRequestReceiverProvider.get());
        receivers.put(RetainedMessageAddReplicateRequest.class, this.retainedMessageReplicateAddRequestReceiverProvider.get());
        receivers.put(RetainedMessageRemoveReplicateRequest.class, this.retainedMessageReplicateRemoveRequestReceiverProvider.get());
        receivers.put(RetainedMessagePutAllRequest.class, this.retainedMessagePutAllRequestReceiverProvider.get());
        receivers.put(RetainedMessageClearRequest.class, this.retainedMessageClearRequestReceiverProvider.get());
        receivers.put(ClusterStateRequest.class, this.stateRequestReceiverProvider.get());
        receivers.put(ClusterStateNotificationRequest.class, this.notificationRequestReceiverProvider.get());
        receivers.put(ReplicateFinishedRequest.class, this.replicationFinishedRequestReceiverProvider.get());
        receivers.put(ClusterContextRequest.class, this.clusterContextRequestReceiverProvider.get());
        receivers.put(NodeInformationRequest.class, this.clusterVersionRequestReceiverProvider.get());
        receivers.put(MetricRequest.class, this.metricRequestReceiverProvider.get());
        receivers.put(AllClusterStatesRequest.class, this.allClusterStatesRequestReceiverProvider.get());
        receivers.put(LicenseCheckRequest.class, this.licenseCheckRequestReceiverProvider.get());
        receivers.put(LicenseInformationRequest.class, this.licenseInformationRequestReceiverProvider.get());
        receivers.put(TopicTreeAddRequest.class, this.topicTreeAddRequestReceiverProvider.get());
        receivers.put(TopicTreeRemoveRequest.class, this.topicTreeRemoveRequestReceiverProvider.get());
        receivers.put(TopicTreeReplicateAddRequest.class, this.topicTreeReplicateAddRequestReceiverProvider.get());
        receivers.put(TopicTreeRemoveReplicateRequest.class, this.topicTreeReplicateRemoveRequestReceiverProvider.get());
        receivers.put(TopicTreeGetRequest.class, this.topicTreeGetRequestReceiverProvider.get());
        receivers.put(TopicTreeReplicateSegmentRequest.class, this.topicTreeReplicateSegmentsRequestReceiverProvider.get());
        receivers.put(ClientSessionGetRequest.class, this.clientSessionGetRequestReceiverProvider.get());
        receivers.put(ClientConnectedRequest.class, this.clientConnectedRequestReceiverProvider.get());
        receivers.put(ClientDisconnectedRequest.class, this.clientDisconnectedRequestReceiverProvider.get());
        receivers.put(ClientSessionPutAllRequest.class, this.clientSessionPutAllRequestReceiverProvider.get());
        receivers.put(ClientSessionReplicateRequest.class, this.clientSessionReplicationRequestReceiverProvider.get());
        receivers.put(ClientTakeoverRequest.class, this.clientTakeoverRequestReceiverProvider.get());
        receivers.put(ConnectedClientsRequest.class, this.connectedClientsRequestReceiverProvider.get());
        receivers.put(DisconnectedClientsRequest.class, this.disconnectedClientsRequestReceiverProvider.get());
        receivers.put(ClientDataRequest.class, this.connectedClientRequestReceiverProvider.get());
        receivers.put(AllClientsRequest.class, this.allClientsRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionGetRequest.class, this.clientSessionSubscriptionGetRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionAddRequest.class, this.clientSessionSubscriptionAddRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionRemoveRequest.class, this.clientSessionSubscriptionRemoveRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionAddReplicateRequest.class, this.clientSessionSubscriptionReplicateAddRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionRemoveReplicateRequest.class, this.clientSessionSubscriptionReplicateRemoveRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionRemoveAllReplicateRequest.class, this.clientSessionSubscriptionReplicateRemoveAllRequestReceiverProvider.get());
        receivers.put(ClientSessionSubscriptionAllReplicateRequest.class, this.clientSessionSubscriptionReplicateAllRequestReceiverProvider.get());
        receivers.put(NodeForPublishRequest.class, this.nodeForPublishRequestReceiverProvider.get());
        receivers.put(ClusterPublishRequest.class, this.clusterPublishRequestReceiverProvider.get());
        receivers.put(ClusterPubRelRequest.class, this.clusterPubRelRequestReceiverProvider.get());
        receivers.put(MessageQueueOfferReplicateRequest.class, this.messageQueueOfferReplicationRequestReceiverProvider.get());
        receivers.put(MessageQueuePollRequest.class, this.messageQueuePollRequestReceiverProvider.get());
        receivers.put(MessageQueueRemoveReplicateRequest.class, this.messageQueueRemoveReplicationRequestReceiverProvider.get());
        receivers.put(MessageQueueDrainedAckRequest.class, this.messageQueueDrainedAckRequestReceiverProvider.get());
        receivers.put(MessageQueueReplicateRequest.class, this.messageQueueReplicationRequestReceiverProvider.get());
        receivers.put(MessageQueueRemoveAllReplicateRequest.class, this.messageQueueRemoveAllReplicateRequestReceiverProvider.get());
        receivers.put(OutgoingMessageFlowPutRequest.class, this.outgoingMessageFlowPutRequestReceiverProvider.get());
        receivers.put(OutgoingMessageFlowPutReplicateRequest.class, this.outgoingMessageFlowPutReplicateRequestReceiverProvider.get());
        receivers.put(OutgoingMessageFlowRemoveAllReplicateRequest.class, this.outgoingMessageFlowRemoveAllReplicationRequestReceiverProvider.get());
        receivers.put(OutgoingMessageFlowReplicateRequest.class, this.outgoingMessageFlowReplicationRequestReceiverProvider.get());
        return new ClusterRequestDispatcherImpl(receivers, this.rpcExecutorProvider.get());
    }

}
