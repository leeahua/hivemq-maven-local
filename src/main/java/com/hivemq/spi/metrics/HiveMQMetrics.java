package com.hivemq.spi.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.hivemq.spi.callback.events.OnConnectCallback;
import com.hivemq.spi.callback.events.OnDisconnectCallback;
import com.hivemq.spi.callback.events.OnPublishReceivedCallback;
import com.hivemq.spi.callback.events.OnPublishSend;
import com.hivemq.spi.callback.events.OnSubscribeCallback;
import com.hivemq.spi.callback.events.OnUnsubscribeCallback;
import com.hivemq.spi.callback.lowlevel.OnConnAckSend;
import com.hivemq.spi.callback.lowlevel.OnPingCallback;
import com.hivemq.spi.callback.lowlevel.OnPubCompReceived;
import com.hivemq.spi.callback.lowlevel.OnPubCompSend;
import com.hivemq.spi.callback.security.AfterLoginCallback;
import com.hivemq.spi.callback.security.OnAuthenticationCallback;
import com.hivemq.spi.callback.security.OnAuthorizationCallback;
import com.hivemq.spi.callback.security.OnInsufficientPermissionDisconnect;
import com.hivemq.spi.callback.security.RestrictionsAfterLoginCallback;
import com.hivemq.spi.services.PluginExecutorService;

/**
 * This class holds a constant {@link HiveMQMetric} for every metric which is provided by HiveMQ
 *
 * @author Christoph Schäbel
 */
public class HiveMQMetrics {

    public static final String PLUGIN_EXECUTOR_PREFIX = "com.hivemq.plugin.executor";
    public static final String CALLBACK_EXECUTOR_PREFIX = "com.hivemq.callback.executor";
    public static final String SINGLE_WRITER_PREFIX = "com.hivemq.persistence.executor";
    public static final String EXCEPTION_PREFIX = "com.hivemq.exceptions";

    /**
     * represents a {@link Counter}, which counts every incoming MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.total.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT message
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.total.count", Counter.class);

    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_MESSAGE_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.total.rate", Meter.class);


    /**
     * represents a {@link Meter},which measures the current rate of outgoing MQTT messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_MESSAGE_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.total.rate", Meter.class);


    /**
     * represents a {@link Histogram}, which measures the distribution of incoming MQTT message size (payload without fixed header)
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> INCOMING_MESSAGE_SIZE_MEAN =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.total.bytes", Histogram.class);

    /**
     * represents a {@link Histogram}, which measures the distribution of incoming MQTT message size (payload without fixed header)
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> OUTGOING_MESSAGE_SIZE_MEAN =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.total.bytes", Histogram.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT Connect messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_CONNECT_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.connect.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT Connect messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_CONNECT_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.connect.count", Counter.class);


    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT ConnAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_CONNACK_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.connack.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT ConnAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_CONNACK_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.connack.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PUBLISH_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.publish.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBLISH_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.publish.count", Counter.class);

    /**
     * represents a {@link Histogram}, which measures the distribution of incoming MQTT message size (payload without fixed header)
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> INCOMING_PUBLISH_SIZE_MEAN =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.publish.bytes", Histogram.class);

    /**
     * represents a {@link Histogram}, which measures the distribution of incoming MQTT message size (payload without fixed header)
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> OUTGOING_PUBLISH_SIZE_MEAN =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.publish.bytes", Histogram.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PUBLISH_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.publish.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBLISH_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.publish.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT Disconnect messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_DISCONNECT_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.disconnect.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT Disconnect messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_DISCONNECT_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.disconnect.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT PingReq messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PINGREQ_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pingreq.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PingReq messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PINGREQ_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pingreq.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of outgoing MQTT PingResp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PINGRESP_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pingresp.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PingResp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PINGRESP_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pingresp.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT PubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PUBACK_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.puback.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBACK_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.puback.count", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT PubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PUBACK_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.puback.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBACK_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.puback.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT PubComp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PUBCOMP_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubcomp.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PubComp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBCOMP_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubcomp.count", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT PubComp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PUBCOMP_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubcomp.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PubComp messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBCOMP_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubcomp.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT PubRec messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PUBREC_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubrec.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PubRec messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBREC_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubrec.count", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT PubRec messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PUBREC_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubrec.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PubRec messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBREC_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubrec.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT PubRel messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_PUBREL_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubrel.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT PubRel messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_PUBREL_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.pubrel.count", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT PubRel messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_PUBREL_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubrel.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT PubRel messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_PUBREL_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.pubrel.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT Subscribe messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_SUBSCRIBE_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.subscribe.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT Subscribe messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_SUBSCRIBE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.subscribe.count", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT SubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_SUBACK_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.suback.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every outgoing MQTT SubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_SUBACK_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.suback.count", Counter.class);


    /**
     * represents a {@link Meter},which measures the current rate of incoming MQTT Unsubscribe messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> INCOMING_UNSUBSCRIBE_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.unsubscribe.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every incoming MQTT Unsubscribe messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> INCOMING_UNSUBSCRIBE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.incoming.unsubscribe.count", Counter.class);

    /**
     * represents a {@link Counter}, which counts the amount of clients with a message queue, that is at least half-full.
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> HALF_FULL_QUEUE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.clients.half-full-queue.count", Counter.class);

    /**
     * represents a {@link Meter},which measures the current rate of dropped Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> DROPPED_MESSAGE_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.dropped.rate", Meter.class);

    /**
     * represents a {@link Counter}, which counts every dropped Publish messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> DROPPED_MESSAGE_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.dropped.count", Counter.class);


    /**
     * represents a {@link Meter}, which measures the current rate of outgoing MQTT UnsubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> OUTGOING_UNSUBACK_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.unsuback.rate", Meter.class);

    /**
     * represents a {@link Gauge}, which holds the current amount of retained messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> RETAINED_MESSAGES_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.messages.retained.current");

    /**
     * represents a {@link Histogram}, which holds metrics about the mean payload-size of retained messages in bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> RETAINED_MESSAGES_MEAN =
            HiveMQMetric.valueOf("com.hivemq.messages.retained.mean", Histogram.class);

    /**
     * represents a {@link Histogram}, which holds the current rate of retained messages
     *
     * @since 3.1
     */
    public static final HiveMQMetric<Meter> RETAINED_MESSAGES_RATE =
            HiveMQMetric.valueOf("com.hivemq.messages.retained.rate", Meter.class);


    /**
     * represents a {@link Counter}, which counts every outgoing MQTT UnsubAck messages
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> OUTGOING_UNSUBACK_COUNT =
            HiveMQMetric.valueOf("com.hivemq.messages.outgoing.unsuback.count", Counter.class);


    /**
     * represents a {@link Gauge}, which holds the current (last 5 seconds) amount of read bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_READ_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.read.current");


    /**
     * represents a {@link Gauge}, which holds the current (last 5 seconds) amount of written bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_WRITE_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.write.current");


    /**
     * represents a {@link Gauge}, which holds the total amount of read bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_READ_TOTAL =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.read.total");


    /**
     * represents a {@link Gauge}, which holds total of written bytes
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> BYTES_WRITE_TOTAL =
            HiveMQMetric.gaugeValue("com.hivemq.networking.bytes.write.total");


    /**
     * represents a {@link Gauge}, which holds the current total number of connections
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CONNECTIONS_OVERALL_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.networking.connections.current");

    /**
     * represents a {@link Gauge}, which holds the mean total number of connections
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> CONNECTIONS_OVERALL_MEAN =
            HiveMQMetric.valueOf("com.hivemq.networking.connections.mean", Histogram.class);


    /**
     * represents a {@link Counter}, which measures the current count of subscriptions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> SUBSCRIPTIONS_CURRENT =
            HiveMQMetric.valueOf("com.hivemq.subscriptions.overall.current", Counter.class);

    /**
     * represents a {@link Counter}, which measures the current count of active persistent sessions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> PERSISTENT_SESSIONS_ACTIVE =
            HiveMQMetric.valueOf("com.hivemq.sessions.persistent.active", Counter.class);


    /**
     * represents a {@link Gauge}, which measures the current count of stored sessions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Gauge<Number>> CLIENT_SESSIONS_CURRENT =
            HiveMQMetric.gaugeValue("com.hivemq.sessions.overall.current");


    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> PLUGIN_EXECUTOR_SERVICE_SUMBITTED =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".submitted", Meter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> PLUGIN_EXECUTOR_SERVICE_RUNNING =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".running", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> PLUGIN_EXECUTOR_SERVICE_COMPLETED =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".completed", Meter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_EXECUTOR_SERVICE_DURATION =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".duration", Timer.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> PLUGIN_EXECUTOR_SERVICE_SCHEDULED_ONCE =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".scheduled.once", Meter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> PLUGIN_EXECUTOR_SERVICE_SCHEDULED_REPETITIVELY =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".scheduled.repetitively", Meter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> PLUGIN_EXECUTOR_SERVICE_SCHEDULED_OVERRUN =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".scheduled.overrun", Counter.class);

    /**
     * represents a {@link Meter}, which measures the current rate of submitted jobs to the
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Histogram> PLUGIN_EXECUTOR_SERVICE_SCHEDULED_PERCENT_OF_PERIOD =
            HiveMQMetric.valueOf(PLUGIN_EXECUTOR_PREFIX + ".scheduled.percent-of-period", Histogram.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link AfterLoginCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_AFTER_LOGIN_SUCCESS =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.after-login.success.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link AfterLoginCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_AFTER_LOGIN_FAILED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.after-login.failed.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnAuthenticationCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_AUTHENTICATION =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.authentication.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link RestrictionsAfterLoginCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_RESTRICTIONS =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.restrictions.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnAuthorizationCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_AUTHORIZATION =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.authorization.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnInsufficientPermissionDisconnect}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PERMISSIONS_DISCONNECT_PUBLISH =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.permissions-disconnect.publish.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnInsufficientPermissionDisconnect}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PERMISSIONS_DISCONNECT_SUBSCRIBE =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.permissions-disconnect.subscribe.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnConnectCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_CONNECT =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.connect.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnDisconnectCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_DISCONNECT =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.disconnect.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPublishReceivedCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBLISH_RECEIVED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.publish-received.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPublishSend}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBLISH_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.publish-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnSubscribeCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_SUBSCRIBE =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.subscribe.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBACK_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.puback-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompReceived} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBACK_RECEIVED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.puback-received.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_SUBACK_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.suback-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompReceived} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_UNSUBACK_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.unsuback-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBCOMP_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubcomp-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompReceived} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBCOMP_RECEIVED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubcomp-received.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBREC_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubrec-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompReceived} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBREC_RECEIVED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubrec-received.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBREL_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubrel-send.time", Timer.class);

    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPubCompReceived} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PUBREL_RECEIVED =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.pubrel-received.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnConnAckSend} callback
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_CONNACK_SEND =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.connack-send.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnUnsubscribeCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_UNSUBSCRIBE =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.unsubscribe.time", Timer.class);


    /**
     * represents a {@link Timer}, which measures the mean execution time (in nanoseconds)
     * of the {@link OnPingCallback}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Timer> PLUGIN_TIMER_PING =
            HiveMQMetric.valueOf("com.hivemq.plugin.callbacks.ping.time", Timer.class);


    /**
     * represents a {@link Meter}, which measures the rate of unhandled Exceptions
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> TOTAL_EXCEPTION_RATE =
            HiveMQMetric.valueOf(EXCEPTION_PREFIX + ".total", Meter.class);


    /**
     * represents a {@link Counter}, which measures the current count of messages in the publish queue
     * {@link PluginExecutorService}
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Counter> PUBLISH_QUEUE_SIZE =
            HiveMQMetric.valueOf("com.hivemq.queues.publish.size", Counter.class);


    /**
     * represents a {@link Meter}, which measures the rate of messages put into the publish queue
     *
     * @since 3.0
     */
    public static final HiveMQMetric<Meter> PUBLISH_QUEUE_RATE =
            HiveMQMetric.valueOf("com.hivemq.queues.publish.rate", Meter.class);

}

