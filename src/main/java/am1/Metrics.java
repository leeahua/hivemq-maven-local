package am1;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.hivemq.spi.metrics.HiveMQMetrics;

import javax.inject.Singleton;

@Singleton
public class Metrics {
    private final MetricRegistry metricRegistry;
    private final Counter incomingMessageCount;
    private final Counter outgoingMessageCount;
    private final Meter incomingMessageRate;
    private final Meter outgoingMessageRate;
    private final Meter outgoingConnAckRate;
    private final Counter outgoingConnAckCount;
    private final Meter incomingConnectRate;
    private final Counter incomingConnectCount;
    private final Meter incomingDisconnectRate;
    private final Counter incomingDisconnectCount;
    private final Meter incomingPingReqRate;
    private final Counter incomingPingReqCount;
    private final Meter outgoingPingRespRate;
    private final Counter outgoingPingRespCount;
    private final Meter incomingPubAckRate;
    private final Counter incomingPubAckCount;
    private final Meter outgoingPubAckRate;
    private final Counter outgoingPubAckCount;
    private final Meter incomingPubCompRate;
    private final Counter incomingPubCompCount;
    private final Meter outgoingPubCompRate;
    private final Counter outgoingPubCompCount;
    private final Meter incomingPublishRate;
    private final Counter incomingPublishCount;
    private final Meter outgoingPublishRate;
    private final Counter outgoingPublishCount;
    private final Meter incomingPubRecRate;
    private final Counter incomingPubRecCount;
    private final Meter outgoingPubRecRate;
    private final Counter outgoingPubRecCount;
    private final Meter incomingPubRelRate;
    private final Counter incomingPubRelCount;
    private final Meter outgoingPubRelRate;
    private final Counter outgoingPubRelCount;
    private final Meter outgoingSubAckRate;
    private final Counter outgoingSubAckCount;
    private final Meter incomingSubscribeRate;
    private final Counter incomingSubscribeCount;
    private final Meter outgoingUnsubAckRate;
    private final Counter outgoingUnsubAckCount;
    private final Meter incomingUnsubscribeRate;
    private final Counter incomingUnsubscribeCount;
    private final Counter halfFullQueueCount;
    private final Meter droppedMessageRate;
    private final Counter droppedMessageCount;
    private final Meter publishQueueRate;
    private final Counter publishQueueSize;
    private final Counter persistentSessionsActive;
    private final Histogram connectionsOverallMean;
    private final Histogram incomingMessageSizeMean;
    private final Histogram outgoingMessageSizeMean;
    private final Histogram incomingPublishSizeMean;
    private final Histogram outgoingPublishSizeMean;
    private final Histogram retainedMessagesMean;
    private final Meter retainedMessagesRate;
    private final Counter subscriptionsCurrent;
    private final Timer pluginTimerAfterLoginSuccess;
    private final Timer pluginTimerAfterLoginFailed;
    private final Timer pluginTimerAuthentication;
    private final Timer pluginTimerRestrictions;
    private final Timer pluginTimerAuthorization;
    private final Timer pluginTimerPermissionsDisconnectPublish;
    private final Timer pluginTimerPermissionsDisconnectSubscribe;
    private final Timer pluginTimerConnect;
    private final Timer pluginTimerDisconnect;
    private final Timer pluginTimerConnAckSend;
    private final Timer pluginTimerPubAckSend;
    private final Timer pluginTimerPubAckReceived;
    private final Timer pluginTimerPubCompReceived;
    private final Timer pluginTimerPublishReceived;
    private final Timer pluginTimerPublishSend;
    private final Timer pluginTimerSubscribe;
    private final Timer pluginTimerUnsubscribe;
    private final Timer pluginTimerPing;
    private final Timer pluginTimerPubRelReceived;
    private final Timer pluginTimerPubRecReceived;
    private final Timer pluginTimerPubCompSend;
    private final Timer pluginTimerPubRecSend;
    private final Timer pluginTimerPubRelSend;
    private final Timer pluginTimerSubAckSend;
    private final Timer pluginTimerUnsubAckSend;
    private final Meter totalExceptionRate;

    public Metrics(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.incomingMessageCount = metricRegistry.counter(HiveMQMetrics.INCOMING_MESSAGE_COUNT.name());
        this.outgoingMessageCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_MESSAGE_COUNT.name());
        this.incomingMessageRate = metricRegistry.meter(HiveMQMetrics.INCOMING_MESSAGE_RATE.name());
        this.outgoingMessageRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_MESSAGE_RATE.name());
        this.outgoingConnAckCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_CONNACK_COUNT.name());
        this.outgoingConnAckRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_CONNACK_RATE.name());
        this.incomingConnectCount = metricRegistry.counter(HiveMQMetrics.INCOMING_CONNECT_COUNT.name());
        this.incomingConnectRate = metricRegistry.meter(HiveMQMetrics.INCOMING_CONNECT_RATE.name());
        this.incomingDisconnectCount = metricRegistry.counter(HiveMQMetrics.INCOMING_DISCONNECT_COUNT.name());
        this.incomingDisconnectRate = metricRegistry.meter(HiveMQMetrics.INCOMING_DISCONNECT_RATE.name());
        this.incomingPingReqCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PINGREQ_COUNT.name());
        this.incomingPingReqRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PINGREQ_RATE.name());
        this.outgoingPingRespCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PINGRESP_COUNT.name());
        this.outgoingPingRespRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PINGRESP_RATE.name());
        this.incomingPubAckCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PUBACK_COUNT.name());
        this.outgoingPubAckCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PUBACK_COUNT.name());
        this.incomingPubAckRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PUBACK_RATE.name());
        this.outgoingPubAckRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PUBACK_RATE.name());
        this.incomingPubCompCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PUBCOMP_COUNT.name());
        this.outgoingPubCompCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PUBCOMP_COUNT.name());
        this.incomingPubCompRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PUBCOMP_RATE.name());
        this.outgoingPubCompRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PUBCOMP_RATE.name());
        this.incomingPublishCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PUBLISH_COUNT.name());
        this.outgoingPublishCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PUBLISH_COUNT.name());
        this.incomingPublishRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PUBLISH_RATE.name());
        this.outgoingPublishRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PUBLISH_RATE.name());
        this.incomingPubRecCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PUBREC_COUNT.name());
        this.outgoingPubRecCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PUBREC_COUNT.name());
        this.incomingPubRecRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PUBREC_RATE.name());
        this.outgoingPubRecRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PUBREC_RATE.name());
        this.incomingPubRelCount = metricRegistry.counter(HiveMQMetrics.INCOMING_PUBREL_COUNT.name());
        this.outgoingPubRelCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_PUBREL_COUNT.name());
        this.incomingPubRelRate = metricRegistry.meter(HiveMQMetrics.INCOMING_PUBREL_RATE.name());
        this.outgoingPubRelRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_PUBREL_RATE.name());
        this.outgoingSubAckCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_SUBACK_COUNT.name());
        this.outgoingSubAckRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_SUBACK_RATE.name());
        this.incomingSubscribeCount = metricRegistry.counter(HiveMQMetrics.INCOMING_SUBSCRIBE_COUNT.name());
        this.incomingSubscribeRate = metricRegistry.meter(HiveMQMetrics.INCOMING_SUBSCRIBE_RATE.name());
        this.outgoingUnsubAckCount = metricRegistry.counter(HiveMQMetrics.OUTGOING_UNSUBACK_COUNT.name());
        this.outgoingUnsubAckRate = metricRegistry.meter(HiveMQMetrics.OUTGOING_UNSUBACK_RATE.name());
        this.incomingUnsubscribeCount = metricRegistry.counter(HiveMQMetrics.INCOMING_UNSUBSCRIBE_COUNT.name());
        this.incomingUnsubscribeRate = metricRegistry.meter(HiveMQMetrics.INCOMING_UNSUBSCRIBE_RATE.name());
        this.halfFullQueueCount = metricRegistry.counter(HiveMQMetrics.HALF_FULL_QUEUE_COUNT.name());
        this.droppedMessageCount = metricRegistry.counter(HiveMQMetrics.DROPPED_MESSAGE_COUNT.name());
        this.droppedMessageRate = metricRegistry.meter(HiveMQMetrics.DROPPED_MESSAGE_RATE.name());
        this.publishQueueSize = metricRegistry.counter(HiveMQMetrics.PUBLISH_QUEUE_SIZE.name());
        this.publishQueueRate = metricRegistry.meter(HiveMQMetrics.PUBLISH_QUEUE_RATE.name());
        this.persistentSessionsActive = metricRegistry.counter(HiveMQMetrics.PERSISTENT_SESSIONS_ACTIVE.name());
        this.connectionsOverallMean = metricRegistry.histogram(HiveMQMetrics.CONNECTIONS_OVERALL_MEAN.name());
        this.incomingPublishSizeMean = metricRegistry.histogram(HiveMQMetrics.INCOMING_PUBLISH_SIZE_MEAN.name());
        this.outgoingPublishSizeMean = metricRegistry.histogram(HiveMQMetrics.OUTGOING_PUBLISH_SIZE_MEAN.name());
        this.incomingMessageSizeMean = metricRegistry.histogram(HiveMQMetrics.INCOMING_MESSAGE_SIZE_MEAN.name());
        this.outgoingMessageSizeMean = metricRegistry.histogram(HiveMQMetrics.OUTGOING_MESSAGE_SIZE_MEAN.name());
        this.retainedMessagesMean = metricRegistry.histogram(HiveMQMetrics.RETAINED_MESSAGES_MEAN.name());
        this.retainedMessagesRate = metricRegistry.meter(HiveMQMetrics.RETAINED_MESSAGES_RATE.name());
        this.subscriptionsCurrent = metricRegistry.counter(HiveMQMetrics.SUBSCRIPTIONS_CURRENT.name());
        this.pluginTimerAfterLoginSuccess = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_AFTER_LOGIN_SUCCESS.name());
        this.pluginTimerAfterLoginFailed = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_AFTER_LOGIN_FAILED.name());
        this.pluginTimerAuthentication = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_AUTHENTICATION.name());
        this.pluginTimerRestrictions = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_RESTRICTIONS.name());
        this.pluginTimerAuthorization = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_AUTHORIZATION.name());
        this.pluginTimerPermissionsDisconnectPublish = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PERMISSIONS_DISCONNECT_PUBLISH.name());
        this.pluginTimerPermissionsDisconnectSubscribe = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PERMISSIONS_DISCONNECT_SUBSCRIBE.name());
        this.pluginTimerConnect = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_CONNECT.name());
        this.pluginTimerDisconnect = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_DISCONNECT.name());
        this.pluginTimerConnAckSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_CONNACK_SEND.name());
        this.pluginTimerPublishReceived = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBLISH_RECEIVED.name());
        this.pluginTimerPublishSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBLISH_SEND.name());
        this.pluginTimerSubscribe = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_SUBSCRIBE.name());
        this.pluginTimerUnsubscribe = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_UNSUBSCRIBE.name());
        this.pluginTimerPubAckSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBACK_SEND.name());
        this.pluginTimerPubAckReceived = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBACK_RECEIVED.name());
        this.pluginTimerPubCompReceived = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBCOMP_RECEIVED.name());
        this.pluginTimerPing = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PING.name());
        this.pluginTimerPubRelReceived = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBREL_RECEIVED.name());
        this.pluginTimerPubRecReceived = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBREC_RECEIVED.name());
        this.pluginTimerPubCompSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBCOMP_SEND.name());
        this.pluginTimerPubRecSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBREC_SEND.name());
        this.pluginTimerPubRelSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_PUBREL_SEND.name());
        this.pluginTimerSubAckSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_SUBACK_SEND.name());
        this.pluginTimerUnsubAckSend = metricRegistry.timer(HiveMQMetrics.PLUGIN_TIMER_UNSUBACK_SEND.name());
        this.totalExceptionRate = metricRegistry.meter(HiveMQMetrics.TOTAL_EXCEPTION_RATE.name());
    }

    public MetricRegistry getMetricRegistry() {
        return this.metricRegistry;
    }

    public Counter incomingMessageCount() {
        return this.incomingMessageCount;
    }

    public Counter outgoingMessageCount() {
        return this.outgoingMessageCount;
    }

    public Meter incomingMessageRate() {
        return this.incomingMessageRate;
    }

    public Meter outgoingMessageRate() {
        return this.outgoingMessageRate;
    }

    public Meter outgoingConnAckRate() {
        return this.outgoingConnAckRate;
    }

    public Counter outgoingConnAckCount() {
        return this.outgoingConnAckCount;
    }

    public Meter incomingConnectRate() {
        return this.incomingConnectRate;
    }

    public Counter incomingConnectCount() {
        return this.incomingConnectCount;
    }

    public Meter incomingDisconnectRate() {
        return this.incomingDisconnectRate;
    }

    public Counter incomingDisconnectCount() {
        return this.incomingDisconnectCount;
    }

    public Meter incomingPingReqRate() {
        return this.incomingPingReqRate;
    }

    public Counter incomingPingReqCount() {
        return this.incomingPingReqCount;
    }

    public Meter outgoingPingRespRate() {
        return this.outgoingPingRespRate;
    }

    public Counter outgoingPingRespCount() {
        return this.outgoingPingRespCount;
    }

    public Meter incomingPubAckRate() {
        return this.incomingPubAckRate;
    }

    public Counter incomingPubAckCount() {
        return this.incomingPubAckCount;
    }

    public Meter outgoingPubAckRate() {
        return this.outgoingPubAckRate;
    }

    public Counter outgoingPubAckCount() {
        return this.outgoingPubAckCount;
    }

    public Meter incomingPubCompRate() {
        return this.incomingPubCompRate;
    }

    public Counter incomingPubCompCount() {
        return this.incomingPubCompCount;
    }

    public Meter outgoingPubCompRate() {
        return this.outgoingPubCompRate;
    }

    public Counter outgoingPubCompCount() {
        return this.outgoingPubCompCount;
    }

    public Meter incomingPublishRate() {
        return this.incomingPublishRate;
    }

    public Counter incomingPublishCount() {
        return this.incomingPublishCount;
    }

    public Meter outgoingPublishRate() {
        return this.outgoingPublishRate;
    }

    public Counter outgoingPublishCount() {
        return this.outgoingPublishCount;
    }

    public Meter incomingPubRecRate() {
        return this.incomingPubRecRate;
    }

    public Counter incomingPubRecCount() {
        return this.incomingPubRecCount;
    }

    public Meter outgoingPubRecRate() {
        return this.outgoingPubRecRate;
    }

    public Counter outgoingPubRecCount() {
        return this.outgoingPubRecCount;
    }

    public Meter incomingPubRelRate() {
        return this.incomingPubRelRate;
    }

    public Counter incomingPubRelCount() {
        return this.incomingPubRelCount;
    }

    public Meter outgoingPubRelRate() {
        return this.outgoingPubRelRate;
    }

    public Counter outgoingPubRelCount() {
        return this.outgoingPubRelCount;
    }

    public Meter outgoingSubAckRate() {
        return this.outgoingSubAckRate;
    }

    public Counter outgoingSubAckCount() {
        return this.outgoingSubAckCount;
    }

    public Meter incomingSubscribeRate() {
        return this.incomingSubscribeRate;
    }

    public Counter incomingSubscribeCount() {
        return this.incomingSubscribeCount;
    }

    public Meter outgoingUnsubAckRate() {
        return this.outgoingUnsubAckRate;
    }

    public Counter outgoingUnsubAckCount() {
        return this.outgoingUnsubAckCount;
    }

    public Meter incomingUnsubscribeRate() {
        return this.incomingUnsubscribeRate;
    }

    public Counter incomingUnsubscribeCount() {
        return this.incomingUnsubscribeCount;
    }

    public Counter halfFullQueueCount() {
        return this.halfFullQueueCount;
    }

    public Meter droppedMessageRate() {
        return this.droppedMessageRate;
    }

    public Counter droppedMessageCount() {
        return this.droppedMessageCount;
    }

    public Counter persistentSessionsActive() {
        return this.persistentSessionsActive;
    }

    public Histogram connectionsOverallMean() {
        return this.connectionsOverallMean;
    }

    public Histogram incomingMessageSizeMean() {
        return this.incomingMessageSizeMean;
    }

    public Histogram outgoingMessageSizeMean() {
        return this.outgoingMessageSizeMean;
    }

    public Histogram incomingPublishSizeMean() {
        return this.incomingPublishSizeMean;
    }

    public Histogram outgoingPublishSizeMean() {
        return this.outgoingPublishSizeMean;
    }

    public Histogram retainedMessagesMean() {
        return this.retainedMessagesMean;
    }

    public Meter retainedMessagesRate() {
        return this.retainedMessagesRate;
    }

    public Counter subscriptionsCurrent() {
        return this.subscriptionsCurrent;
    }

    public Timer pluginTimerAfterLoginSuccess() {
        return this.pluginTimerAfterLoginSuccess;
    }

    public Timer pluginTimerAfterLoginFailed() {
        return this.pluginTimerAfterLoginFailed;
    }

    public Timer pluginTimerAuthentication() {
        return this.pluginTimerAuthentication;
    }

    public Timer pluginTimerRestrictions() {
        return this.pluginTimerRestrictions;
    }

    public Timer pluginTimerAuthorization() {
        return this.pluginTimerAuthorization;
    }

    public Timer pluginTimerPermissionsDisconnectPublish() {
        return this.pluginTimerPermissionsDisconnectPublish;
    }

    public Timer pluginTimerPermissionsDisconnectSubscribe() {
        return this.pluginTimerPermissionsDisconnectSubscribe;
    }

    public Timer pluginTimerConnect() {
        return this.pluginTimerConnect;
    }

    public Timer pluginTimerDisconnect() {
        return this.pluginTimerDisconnect;
    }

    public Timer pluginTimerConnAckSend() {
        return this.pluginTimerConnAckSend;
    }

    public Timer pluginTimerPubAckSend() {
        return this.pluginTimerPubAckSend;
    }

    public Timer pluginTimerPubAckReceived() {
        return this.pluginTimerPubAckReceived;
    }

    public Timer pluginTimerPublishReceived() {
        return this.pluginTimerPublishReceived;
    }

    public Timer pluginTimerPublishSend() {
        return this.pluginTimerPublishSend;
    }

    public Timer pluginTimerSubscribe() {
        return this.pluginTimerSubscribe;
    }

    public Timer pluginTimerUnsubscribe() {
        return this.pluginTimerUnsubscribe;
    }

    public Timer pluginTimerPing() {
        return this.pluginTimerPing;
    }

    public Timer pluginTimerPubCompReceived() {
        return this.pluginTimerPubCompReceived;
    }

    public Meter totalExceptionRate() {
        return this.totalExceptionRate;
    }

    public Timer pluginTimerPubRelReceived() {
        return this.pluginTimerPubRelReceived;
    }

    public Timer pluginTimerPubRecReceived() {
        return this.pluginTimerPubRecReceived;
    }

    public Timer pluginTimerPubCompSend() {
        return this.pluginTimerPubCompSend;
    }

    public Timer pluginTimerPubRecSend() {
        return this.pluginTimerPubRecSend;
    }

    public Timer pluginTimerPubRelSend() {
        return this.pluginTimerPubRelSend;
    }

    public Timer pluginTimerSubAckSend() {
        return this.pluginTimerSubAckSend;
    }

    public Timer pluginTimerUnsubAckSend() {
        return this.pluginTimerUnsubAckSend;
    }

    public Meter publishQueueRate() {
        return this.publishQueueRate;
    }

    public Counter publishQueueSize() {
        return this.publishQueueSize;
    }
}
