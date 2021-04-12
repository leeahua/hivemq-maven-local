package bj;

import cb1.AttributeKeys;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class MqttConnAckSentFutureListener implements ChannelFutureListener {

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
        if (future.isSuccess()) {
            future.channel().attr(AttributeKeys.MQTT_CONNACK_SENT).set(true);
        }
    }
}
