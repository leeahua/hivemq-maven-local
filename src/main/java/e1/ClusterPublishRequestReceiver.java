package e1;

import a1.ClusterReceiver;
import ab.ClusterResponse;
import ab.ClusterResponseCode;
import aj.ClusterFutures;
import av.InternalConfigurationService;
import av.Internals;
import bl1.ChannelPersistence;
import bm1.QueuedMessagesSinglePersistence;
import bo.ClusterPublish;
import bo.SendStatus;
import bu.InternalPublish;
import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import cb1.AttributeKeys;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.google.inject.Inject;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.message.QoS;
import d.CacheScoped;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import q.ConsistentHashingRing;
import s.Primary;
import t.ClusterConnection;
import t1.ClientDisconnectedRequest;
import v.ClientSession;
import v.ClientSessionClusterPersistence;
import w.QueuedMessagesClusterPersistence;
import w1.ClusterPublishRequest;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

// TODO:
@CacheScoped
public class ClusterPublishRequestReceiver
        implements ClusterReceiver<ClusterPublishRequest> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClusterPublishRequestReceiver.class);
    private final ClientSessionClusterPersistence clientSessionClusterPersistence;
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final QueuedMessagesClusterPersistence queuedMessagesClusterPersistence;
    private final ConsistentHashingRing primaryRing;
    private final ClusterConnection clusterConnection;
    private final ChannelPersistence channelPersistence;
    private final MessageIDPools messageIDPools;
    private final Cache<String, SendStatus> publishSendStatusCache;

    @Inject
    ClusterPublishRequestReceiver(ClientSessionClusterPersistence clientSessionClusterPersistence,
                                  QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
                                  QueuedMessagesClusterPersistence queuedMessagesClusterPersistence,
                                  @Primary ConsistentHashingRing primaryRing,
                                  ClusterConnection clusterConnection,
                                  ChannelPersistence channelPersistence,
                                  MessageIDPools messageIDPools,
                                  InternalConfigurationService internalConfigurationService) {
        this.clientSessionClusterPersistence = clientSessionClusterPersistence;
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.queuedMessagesClusterPersistence = queuedMessagesClusterPersistence;
        this.primaryRing = primaryRing;
        this.clusterConnection = clusterConnection;
        this.channelPersistence = channelPersistence;
        this.messageIDPools = messageIDPools;
        this.publishSendStatusCache = CacheBuilder.newBuilder()
                .expireAfterAccess(internalConfigurationService.getInt(Internals.CLUSTER_PUBLISH_ID_CACHE_DURATION), TimeUnit.SECONDS)
                .maximumSize(internalConfigurationService.getLong(Internals.CLUSTER_PUBLISH_ID_CACHE_SIZE))
                .recordStats()
                .concurrencyLevel(internalConfigurationService.getInt(Internals.CLUSTER_THREAD_POOL_SIZE_MAXIMUM))
                .build();
    }

    public void received(@NotNull ClusterPublishRequest request, @NotNull ClusterResponse response, @NotNull String sender) {
        Preconditions.checkNotNull(request, "Request must not be null");
        Preconditions.checkNotNull(response, "Response must not be null");
        Preconditions.checkNotNull(sender, "Sender must not be null");
        String clientId = request.getClientId();
        InternalPublish publish = InternalPublish.of(request.getPublish());
        LOGGER.trace("Received cluster Publish for client '{}' from node {}.", clientId, sender);
        String publishKey = clientId + "_" + publish.getUniqueId();
        SendStatus status = this.publishSendStatusCache.getIfPresent(publishKey);
        if (status != null && !request.f()) {
            if (!request.d()) {
                response.sendResult(SendStatus.DELIVERED);
            } else {
                switch (status) {
                    case DELIVERED:
                        response.sendResult(SendStatus.DELIVERED);
                        break;
                    case NOT_CONNECTED:
                        response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE, SendStatus.NOT_CONNECTED);
                        break;
                    case MESSAGE_IDS_EXHAUSTED:
                        response.sendResult(ClusterResponseCode.DENIED, SendStatus.MESSAGE_IDS_EXHAUSTED);
                        break;
                    case QUEUED:
                        response.sendResult(SendStatus.QUEUED);
                        break;
                    case FAILED:
                        response.sendResult(ClusterResponseCode.DENIED, SendStatus.FAILED);
                        break;
                    case IN_PROGRESS:
                        response.sendResult(ClusterResponseCode.BUSY, SendStatus.IN_PROGRESS);
                        break;
                    case PERSISTENT_SESSION:
                        response.sendResult(ClusterResponseCode.DENIED, SendStatus.PERSISTENT_SESSION);
                }
            }
            return;
        }
        this.publishSendStatusCache.put(publishKey, SendStatus.IN_PROGRESS);
        publish.setRetain(false);
        int qoSNumber = Math.min(publish.getQoS().getQosNumber(), request.getQoSNumber());
        if (qoSNumber != publish.getQoS().getQosNumber()) {
            publish.setQoS(QoS.valueOf(qoSNumber));
        }
        if (qoSNumber > 0) {
            int messageId;
            try {
                messageId = getMessageId(request, publish);
            } catch (ProduceMessageIdException e) {
                this.publishSendStatusCache.put(publishKey, SendStatus.MESSAGE_IDS_EXHAUSTED);
                response.sendResult(SendStatus.MESSAGE_IDS_EXHAUSTED);
                return;
            }
            publish.setMessageId(messageId);
        } else {
            publish.setMessageId(0);
        }
        String node = this.primaryRing.getNode(clientId);
        boolean sameNode = node.equals(this.clusterConnection.getClusterId());
        boolean atMostOnce = qoSNumber == 0 || publish.getQoS() == QoS.AT_MOST_ONCE;
        Channel channel = this.channelPersistence.getChannel(clientId);
        boolean channelIsPersist = channel != null;
        if (!sameNode && !channelIsPersist) {
            ClientDisconnectedRequest disconnectedRequest = new ClientDisconnectedRequest(clientId, this.clusterConnection.getClusterId());
            this.clusterConnection.send(disconnectedRequest, node, Void.class);
            a(response, publishKey);
            return;
        }
        boolean isConnAckSent = isConnAckSent(channel);
        if (!sameNode && !isConnAckSent) {
            a(response, publishKey);
            return;
        }
        if (!sameNode) {
            a(request, publish, response, channel, publishKey);
            return;
        }
        boolean bool5 = this.queuedMessagesClusterPersistence.isEmpty(clientId);
        if (!bool5 && !atMostOnce) {
            a(request, response, clientId, publishKey);
            return;
        }
        if (channelIsPersist && isConnAckSent) {
            a(request, publish, response, channel, publishKey);
            return;
        }
        ListenableFuture<ClientSession> localClientSessionFuture = getLocalClientSession(clientId);
        ClusterFutures.addCallback(localClientSessionFuture, new FutureCallback<ClientSession>() {

            @Override
            public void onSuccess(@Nullable ClientSession result) {
                boolean isConnected = result != null && result.isConnected();
                if (isConnected && !channelIsPersist) {
                    a(response, publishKey);
                    return;
                }
                if (request.d()) {
                    publishSendStatusCache.invalidate(publishKey);
                    response.sendResult(ClusterResponseCode.DENIED, SendStatus.PERSISTENT_SESSION);
                    return;
                }
                boolean isPersistentSession = result != null && result.isPersistentSession();
                if (isPersistentSession && !atMostOnce) {
                    a(request, response, clientId, publishKey);
                    return;
                }
                publishSendStatusCache.put(publishKey, SendStatus.DELIVERED);
                response.sendResult(SendStatus.DELIVERED);
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.error("Not able to check client session for publish", t);
                publishSendStatusCache.put(publishKey, SendStatus.FAILED);
                response.sendResult(ClusterResponseCode.FAILED, SendStatus.FAILED);
            }
        });
    }

    private int getMessageId(ClusterPublishRequest request, InternalPublish publish) throws ProduceMessageIdException {
        if (!request.e()) {
            return this.messageIDPools.forClient(request.getClientId()).next();
        }
        return this.messageIDPools.forClient(request.getClientId()).next(publish.getMessageId());
    }

    private void a(@NotNull ClusterPublishRequest request,
                   @NotNull ClusterResponse response,
                   String clientId,
                   String publishKey) {
        this.queuedMessagesSinglePersistence.offer(clientId, request.getPublish());
        this.publishSendStatusCache.put(publishKey, SendStatus.QUEUED);
        response.sendResult(SendStatus.QUEUED);
    }

    private void a(@NotNull ClusterResponse response, String publishKey) {
        this.publishSendStatusCache.invalidate(publishKey);
        response.sendResult(ClusterResponseCode.NOT_RESPONSIBLE, SendStatus.NOT_CONNECTED);
    }

    private void a(@NotNull ClusterPublishRequest request,
                   InternalPublish publish,
                   @NotNull ClusterResponse response,
                   Channel channel,
                   String publishKey) {
        if (!request.d()) {
            this.publishSendStatusCache.put(publishKey, SendStatus.DELIVERED);
            response.sendResult(SendStatus.DELIVERED);
            channel.pipeline().fireUserEventTriggered(publish);
            return;
        }
        SettableFuture<SendStatus> settableFuture = SettableFuture.create();
        channel.pipeline().fireUserEventTriggered(new ClusterPublish(publish, settableFuture));
        ClusterFutures.addCallback(settableFuture, new FutureCallback<SendStatus>() {

            @Override
            public void onSuccess(@Nullable SendStatus result) {
                ClusterResponseCode code;
                if (result == null) {
                    code = ClusterResponseCode.DENIED;
                } else {
                    switch (result) {
                        case DELIVERED:
                        case QUEUED:
                            code = ClusterResponseCode.OK;
                            break;
                        case NOT_CONNECTED:
                        case MESSAGE_IDS_EXHAUSTED:
                        case FAILED:
                            code = ClusterResponseCode.DENIED;
                            break;
                        case PERSISTENT_SESSION:
                            code = ClusterResponseCode.DENIED;
                            break;
                        case IN_PROGRESS:
                        default:
                            code = ClusterResponseCode.DENIED;
                    }
                }
                if (request.d()) {
                    if (result != null) {
                        if (result == SendStatus.PERSISTENT_SESSION) {
                            publishSendStatusCache.invalidate(publishKey);
                        } else {
                            publishSendStatusCache.put(publishKey, result);
                        }
                    } else {
                        publishSendStatusCache.put(publishKey, SendStatus.FAILED);
                    }
                    response.sendResult(code, result);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                LOGGER.trace("Not able to send message from cluster", t);
                if (request.d()) {
                    publishSendStatusCache.put(publishKey, SendStatus.FAILED);
                    response.sendResult(ClusterResponseCode.FAILED, SendStatus.FAILED);
                }
            }
        });
    }

    private boolean isConnAckSent(Channel channel) {
        Boolean connAckSent = channel != null ? channel.attr(AttributeKeys.MQTT_CONNACK_SENT).get() : false;
        return Optional.ofNullable(connAckSent).orElse(false);
    }

    private ListenableFuture<ClientSession> getLocalClientSession(String clientId) {
        ListenableFuture<ClientSession> future = this.clientSessionClusterPersistence.getLocally(clientId);
        SettableFuture<ClientSession> settableFuture = SettableFuture.create();
        ClusterFutures.addCallback(future, new FutureCallback<ClientSession>() {

            @Override
            public void onSuccess(@Nullable ClientSession result) {
                if (result == null) {
                    settableFuture.set(null);
                    return;
                }
                settableFuture.set(result);
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        });
        return settableFuture;
    }
}
