package ap1;

import aq1.MqttConnectionCounterHandler;
import e.Pipelines;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import javax.inject.Inject;

public class StatisticsInitializer extends ChannelHandlerAdapter {
    private final GlobalTrafficCounter globalTrafficCounter;
    private final GlobalMqttMessageCounter globalMqttMessageCounter;
    private final MqttConnectionCounterHandler mqttConnectionCounterHandler;

    @Inject
    StatisticsInitializer(GlobalTrafficCounter globalTrafficCounter,
                          GlobalMqttMessageCounter globalMqttMessageCounter,
                          MqttConnectionCounterHandler mqttConnectionCounterHandler) {
        this.globalTrafficCounter = globalTrafficCounter;
        this.globalMqttMessageCounter = globalMqttMessageCounter;
        this.mqttConnectionCounterHandler = mqttConnectionCounterHandler;
    }

    public void handlerAdded(ChannelHandlerContext ctx) {
        ctx.pipeline().addAfter(Pipelines.ALL_CHANNEL_GROUP_HANDLER, Pipelines.GLOBAL_TRAFFIC_COUNTER, this.globalTrafficCounter);
        ctx.pipeline().addAfter(Pipelines.MQTT_MESSAGE_ENCODER, Pipelines.GLOBAL_MQTT_MESSAGE_COUNTER, this.globalMqttMessageCounter);
        ctx.pipeline().addAfter(Pipelines.GLOBAL_MQTT_MESSAGE_COUNTER, Pipelines.MQTT_CONNECTION_COUNTER_HANDLER, this.mqttConnectionCounterHandler);
        ctx.pipeline().remove(this);
    }
}
