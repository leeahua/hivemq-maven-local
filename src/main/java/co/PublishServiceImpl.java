package co;

import ai.TopicSubscribers;
import am1.Metrics;
import bl1.ChannelPersistence;
import bm1.ClientSessionSinglePersistence;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import bm1.QueuedMessagesSinglePersistence;
import bo.ClusterPublish;
import bo.ClusterPublishCallback;
import bo.ClusterPublishResult;
import bo.DeliveryMessageToClientFailedTask;
import bo.SendStatus;
import bu.InternalPublish;
import bu.MessageIDPools;
import bw.ProduceMessageIdException;
import bx.PublishTopicMatcher;
import bx.SubscriberWithQoS;
import by.TopicTreeSinglePersistence;
import bz.RetainedMessage;
import cb1.AttributeKeys;
import cb1.PluginExceptionUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.hivemq.spi.message.Publish;
import com.hivemq.spi.message.QoS;
import com.hivemq.spi.message.Topic;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.PublishService;
import com.hivemq.spi.topic.TopicMatcher;
import d.CacheScoped;
import i.ClusterConfigurationService;
import i.ClusterIdProducer;
import i.PublishDispatcher;
import io.netty.channel.Channel;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import x.RetainedMessagesSinglePersistence;
import y.ClientSubscriptions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

// TODO:
@CacheScoped
public class PublishServiceImpl
        implements InternalPublishService, PublishService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PublishServiceImpl.class);
    private final ClientSessionSinglePersistence clientSessionSinglePersistence;
    private final ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence;
    private final QueuedMessagesSinglePersistence queuedMessagesSinglePersistence;
    private final ChannelPersistence channelPersistence;
    private final EventExecutorGroup eventExecutorGroup;
    private final RetainedMessagesSinglePersistence retainedMessagesSinglePersistence;
    private final ClusterConfigurationService clusterConfigurationService;
    private final TopicTreeSinglePersistence topicTreeSinglePersistence;
    private final SharedSubscriptionServiceImpl sharedSubscriptionService;
    private final Metrics metrics;
    private final Provider<PublishDispatcher> publishDispatcherProvider;
    private final MessageIDPools messageIDPools;
    private final ClusterIdProducer clusterIdProducer;
    private final PluginExecutorService pluginExecutorService;
    private final TopicMatcher topicMatcher = new PublishTopicMatcher();
    @VisibleForTesting
    ListeningExecutorService retainedPersistenceExecutorService;

    @Inject
    public PublishServiceImpl(ClientSessionSinglePersistence clientSessionSinglePersistence,
                              ClientSessionSubscriptionsSinglePersistence clientSessionSubscriptionsSinglePersistence,
                              QueuedMessagesSinglePersistence queuedMessagesSinglePersistence,
                              ChannelPersistence channelPersistence,
                              EventExecutorGroup eventExecutorGroup,
                              RetainedMessagesSinglePersistence retainedMessagesSinglePersistence,
                              ClusterConfigurationService clusterConfigurationService,
                              TopicTreeSinglePersistence topicTreeSinglePersistence,
                              SharedSubscriptionServiceImpl sharedSubscriptionService,
                              Metrics metrics,
                              Provider<PublishDispatcher> publishDispatcherProvider,
                              MessageIDPools messageIDPools,
                              ClusterIdProducer clusterIdProducer,
                              PluginExecutorService pluginExecutorService) {
        this.clientSessionSinglePersistence = clientSessionSinglePersistence;
        this.clientSessionSubscriptionsSinglePersistence = clientSessionSubscriptionsSinglePersistence;
        this.queuedMessagesSinglePersistence = queuedMessagesSinglePersistence;
        this.channelPersistence = channelPersistence;
        this.eventExecutorGroup = eventExecutorGroup;
        this.retainedMessagesSinglePersistence = retainedMessagesSinglePersistence;
        this.clusterConfigurationService = clusterConfigurationService;
        this.topicTreeSinglePersistence = topicTreeSinglePersistence;
        this.sharedSubscriptionService = sharedSubscriptionService;
        this.metrics = metrics;
        this.publishDispatcherProvider = publishDispatcherProvider;
        this.messageIDPools = messageIDPools;
        this.clusterIdProducer = clusterIdProducer;
        this.pluginExecutorService = pluginExecutorService;
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("retained-persistence-writer-%d").build();
        this.retainedPersistenceExecutorService = MoreExecutors.listeningDecorator(Executors.newSingleThreadExecutor(threadFactory));
    }

    public void publish(Publish publish) {
        publish(publish, this.pluginExecutorService);
    }

    public void publish(Publish publish, ExecutorService executorService) {
        if (publish instanceof InternalPublish) {
            publish((InternalPublish) publish, executorService);
        } else {
            InternalPublish internalPublish = new InternalPublish(this.clusterIdProducer.get(), publish);
            publish(internalPublish, executorService);
        }
    }

    public void publishToClient(Publish publish, String clientId) {
        publishToClient(publish, clientId, this.pluginExecutorService);
    }

    public void publishToClient(Publish publish, String clientId, ExecutorService executorService) {
        Preconditions.checkNotNull(publish);
        Preconditions.checkNotNull(clientId);
        Preconditions.checkNotNull(publish.getTopic());
        Preconditions.checkNotNull(publish.getQoS());
        Preconditions.checkNotNull(publish.getPayload());
        if (publish instanceof InternalPublish) {
            publishToClient((InternalPublish) publish, clientId, executorService);
        } else {
            InternalPublish internalPublish = new InternalPublish(this.clusterIdProducer.get(), publish);
            publishToClient(internalPublish, clientId, executorService);
        }
    }

    protected void publishToClient(InternalPublish publish, String clientId, ExecutorService executorService) {
        ListenableFuture<ClientSubscriptions> subscriptionsFuture = this.clientSessionSubscriptionsSinglePersistence.getSubscriptions(clientId);
        Futures.addCallback(subscriptionsFuture, new FutureCallback<ClientSubscriptions>() {

            @Override
            public void onSuccess(@Nullable ClientSubscriptions result) {
                Iterator<Topic> subscriptions = result.getSubscriptions().iterator();
                while (subscriptions.hasNext()) {
                    Topic subscription = subscriptions.next();
                    if (topicMatcher.matches(subscription.getTopic(), publish.getTopic())) {
                        if (isActive(clientId)) {
                            a(publish, clientId, subscription.getQoS().getQosNumber(), false, executorService);
                            break;
                        }
                        if (!clusterConfigurationService.isEnabled() ||
                                publish.getTopic().startsWith("$SYS")) {
                            break;
                        }
                        publishDispatcherProvider.get().dispatch(publish, clientId,
                                subscription.getQoS().getQosNumber(), false, false, false, executorService);
                        break;
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                PluginExceptionUtils.logOrThrow("Not able to publish message " +
                        publish.getUniqueId() + " to client " + clientId + ".", t);
            }
        }, this.eventExecutorGroup);
    }

    public void publish(InternalPublish publish, ExecutorService executorService) {
        if (publish.isRetain()) {
            ListenableFuture<Void> retainedMessageFuture;
            if (publish.getPayload().length > 0) {
                RetainedMessage retainedMessage = new RetainedMessage(publish.getPayload(), publish.getQoS());
                LOGGER.trace("Adding retained message on topic {}", publish.getTopic());
                retainedMessageFuture = this.retainedMessagesSinglePersistence.addOrReplace(publish.getTopic(), retainedMessage);
            } else {
                LOGGER.trace("Deleting retained message on topic {}", publish.getTopic());
                retainedMessageFuture = this.retainedMessagesSinglePersistence.remove(publish.getTopic());
            }
            Futures.addCallback(retainedMessageFuture, new FutureCallback<Void>() {

                @Override
                public void onSuccess(@Nullable Void result) {
                    metrics.retainedMessagesMean().update(publish.getPayload().length);
                    metrics.retainedMessagesRate().mark();
                }

                @Override
                public void onFailure(Throwable t) {
                    PluginExceptionUtils.logOrThrow("Unable able to store retained message for topic " +
                            publish.getTopic() + " with message id " + publish.getUniqueId() + ".", t);
                }
            }, executorService);
        }
        doPublish(publish, executorService);
    }

    private void doPublish(InternalPublish publish, ExecutorService executorService) {
        ListenableFuture<TopicSubscribers> subscribersFuture = this.topicTreeSinglePersistence.getSubscribers(publish.getTopic());
        Futures.addCallback(subscribersFuture, new FutureCallback<TopicSubscribers>() {

            @Override
            public void onSuccess(@Nullable TopicSubscribers result) {
                if (result == null ||
                        result.getSubscribers() == null ||
                        result.getSubscribers().size() < 1) {
                    return;
                }
                Map<String, List<SubscriberWithQoS>> subscribers = null;
                Iterator<SubscriberWithQoS> iterator = result.getSubscribers().iterator();
                while (iterator.hasNext()) {
                    SubscriberWithQoS subscriber = iterator.next();
                    if (!subscriber.isShared()) {
                        ListenableFuture<ClusterPublishResult> f = a(publish, subscriber.getSubscriber(), subscriber.getQosNumber(), false, pluginExecutorService);
                        Futures.addCallback(f, new a(metrics, subscriber.getSubscriber(), publish), pluginExecutorService);
                    } else {
                        if (subscribers == null) {
                            subscribers = new HashMap<>(result.getSubscribers().size());
                        }
                        if (subscribers.get(subscriber.getGroupId()) == null) {
                            subscribers.put(subscriber.getGroupId(), new ArrayList<>());
                        }
                        subscribers.get(subscriber.getGroupId()).add(subscriber);
                    }
                    if (subscribers != null) {
                        a(publish, subscribers, pluginExecutorService);
                    }
                }
            }

            @Override
            public void onFailure(Throwable t) {
                PluginExceptionUtils.logOrThrow("Unable to publish message for topic " + publish.getTopic() +
                        " with message id" + publish.getUniqueId() + ".", t);
            }
        }, executorService);
    }

    public ListenableFuture<ClusterPublishResult> a(InternalPublish publish,
                                                    String clientId,
                                                    int qoSNumber,
                                                    boolean paramBoolean,
                                                    ExecutorService executorService) {
        if (this.clusterConfigurationService.isEnabled()) {
            return this.publishDispatcherProvider.get().dispatch(publish, clientId, qoSNumber, paramBoolean, false, false, executorService);
        }
        SettableFuture<ClusterPublishResult> settableFuture = SettableFuture.create();
        ListenableFuture<SendStatus> future = a(publish, clientId, qoSNumber, paramBoolean);
        Futures.addCallback(future, new FutureCallback<SendStatus>() {

            @Override
            public void onSuccess(@Nullable SendStatus result) {
                settableFuture.set(new ClusterPublishResult(result, clientId));
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, executorService);
        return settableFuture;
    }

    private ListenableFuture<SendStatus> a(InternalPublish publish,
                                           String clientId,
                                           int qoSNumber,
                                           boolean paramBoolean) {
        Channel channel = this.channelPersistence.getChannel(clientId);


        int i1 = channel != null ? 1 : 0;
        Boolean localBoolean = i1 != 0 ? channel.attr(AttributeKeys.MQTT_CONNACK_SENT).get() : Boolean.FALSE;
        int i2 = localBoolean != null ? localBoolean.booleanValue() : 0;
        if ((i1 != 0) && (i2 != 0)) {
            return a(publish, clientId, qoSNumber, channel);
        }
        return b(publish, clientId, qoSNumber, paramBoolean);
    }

    private ListenableFuture<SendStatus> b(InternalPublish publish,
                                           String clientId,
                                           int qoSNumber,
                                           boolean paramBoolean) {
        if (publish.getQoS().getQosNumber() < 1 || qoSNumber < 1) {
            return Futures.immediateFuture(null);
        }
        InternalPublish copyPublish = InternalPublish.of(publish);
        copyPublish.setQoS(QoS.valueOf(Math.min(qoSNumber, publish.getQoS().getQosNumber())));
        SettableFuture<SendStatus> settableFuture = SettableFuture.create();
        ListenableFuture<Boolean> future = this.clientSessionSinglePersistence.isPersistent(clientId);
        Futures.addCallback(future, new FutureCallback<Boolean>() {

            @Override
            public void onSuccess(@Nullable Boolean result) {
                if (paramBoolean) {
                    if (result) {
                        settableFuture.set(SendStatus.PERSISTENT_SESSION);
                    } else {
                        settableFuture.set(SendStatus.NOT_CONNECTED);
                    }
                    return;
                }
                if (result) {
                    eventExecutorGroup.submit(new DeliveryMessageToClientFailedTask(queuedMessagesSinglePersistence, clientId, copyPublish, settableFuture));
                } else {
                    settableFuture.set(null);
                }
            }

            @Override
            public void onFailure(Throwable t) {
                settableFuture.setException(t);
            }
        }, this.eventExecutorGroup);
        return settableFuture;
    }

    private ListenableFuture<SendStatus> a(InternalPublish publish, String clientId, int qoSNumber, Channel channel) {
        InternalPublish publishForClient = createPublishForClient(publish, clientId, qoSNumber);
        if (publishForClient == null) {
            return Futures.immediateFuture(SendStatus.MESSAGE_IDS_EXHAUSTED);
        }
        SettableFuture<SendStatus> settableFuture = SettableFuture.create();
        channel.pipeline().fireUserEventTriggered(new ClusterPublish(publishForClient, settableFuture));
        return settableFuture;
    }

    private void a(InternalPublish publish, Map<String, List<SubscriberWithQoS>> sharedSubscribers, ExecutorService executorService) {
        sharedSubscribers.forEach((groupId, subscribers) -> {
            Collections.sort(subscribers);
            ArrayList localArrayList = Lists.newArrayList();
            SubscriberWithQoS subscriber = this.sharedSubscriptionService.nextSubscriber(localArrayList, subscribers, groupId);
            ListenableFuture<ClusterPublishResult> future = a(publish, subscriber.getSubscriber(), subscriber.getQosNumber(), true, executorService);
            Futures.addCallback(future,
                    new ClusterPublishCallback(localArrayList, subscribers, publish, subscriber,
                            this.sharedSubscriptionService, this, this.metrics,
                            this.clusterIdProducer, executorService),
                    executorService);
        });
    }

    @Nullable
    private InternalPublish createPublishForClient(InternalPublish publish,
                                                   String clientId,
                                                   int qoSNumber) {
        if (publish.getQoS().getQosNumber() == 0 && !publish.isRetain() && qoSNumber == 0) {
            return publish;
        }
        InternalPublish newPublish = InternalPublish.of(publish, this.clusterIdProducer.get());
        int publishQosNumber = Math.min(qoSNumber, publish.getQoS().getQosNumber());
        newPublish.setQoS(QoS.valueOf(publishQosNumber));
        newPublish.setRetain(false);
        if (newPublish.getQoS().getQosNumber() > QoS.AT_MOST_ONCE.getQosNumber()) {
            try {
                int messageId = this.messageIDPools.forClient(clientId).next();
                newPublish.setMessageId(messageId);
            } catch (ProduceMessageIdException e) {
                return null;
            }
        } else {
            newPublish.setMessageId(0);
        }
        return newPublish;
    }

    private boolean isActive(String clientId) {
        Channel channel = this.channelPersistence.getChannel(clientId);
        return channel != null && channel.isActive();
    }

    private static class a implements FutureCallback<ClusterPublishResult> {
        private final Metrics metrics;
        private final String clientId;
        private final InternalPublish publish;

        private a(Metrics metrics, String clientId, InternalPublish publish) {
            this.metrics = metrics;
            this.clientId = clientId;
            this.publish = publish;
        }

        @Override
        public void onSuccess(@Nullable ClusterPublishResult result) {
            if (result == null) {
                return;
            }
            if (result.getStatus() == null) {
                return;
            }
            switch (k1 .1.a[result.getStatus().ordinal()])
            {
                case 1:
                case 2:
                    break;
                case 3:
                case 4:
                case 5:
                case 6:
                    LOGGER.debug("Message for client {} dropped, reason: {}", this.clientId, result.getStatus());
                    this.metrics.droppedMessageCount().inc();
                    this.metrics.droppedMessageRate().mark();
            }
        }

        public void onFailure(Throwable t) {
            PluginExceptionUtils.logOrThrow("Unable to send message with id " + this.publish + " on topic " + this.publish.getTopic() + " to client " + this.clientId + "", t);
            this.metrics.droppedMessageCount().inc();
            this.metrics.droppedMessageRate().mark();
        }
    }
}
