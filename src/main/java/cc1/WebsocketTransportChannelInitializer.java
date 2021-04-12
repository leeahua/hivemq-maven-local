package cc1;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import e.Pipelines;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

public class WebsocketTransportChannelInitializer {
    public static final int MAX_CONTENT_LENGTH = 65536;
    private final WebsocketListener listener;

    public WebsocketTransportChannelInitializer(WebsocketListener listener) {
        this.listener = listener;
    }

    public void initChannel(Channel channel) {
        channel.pipeline().addBefore(Pipelines.ALL_CHANNEL_GROUP_HANDLER, Pipelines.HTTP_SERVER_CODEC, new HttpServerCodec());
        channel.pipeline().addAfter(Pipelines.HTTP_SERVER_CODEC, Pipelines.HTTP_OBJECT_AGGREGATOR, new HttpObjectAggregator(MAX_CONTENT_LENGTH));
        String path = this.listener.getPath();
        String subProtocols = getSubProtocols();
        boolean allowExtensions = this.listener.getAllowExtensions();
        channel.pipeline().addAfter(Pipelines.HTTP_OBJECT_AGGREGATOR, Pipelines.WEBSOCKET_SERVER_PROTOCOL_HANDLER, new WebSocketServerProtocolHandler(path, subProtocols, allowExtensions, Integer.MAX_VALUE));
        channel.pipeline().addAfter(Pipelines.WEBSOCKET_SERVER_PROTOCOL_HANDLER, Pipelines.WEBSOCKET_BINARY_FRAME_HANDLER, new WebsocketBinaryFrameHandler());
        channel.pipeline().addAfter(Pipelines.WEBSOCKET_BINARY_FRAME_HANDLER, Pipelines.WEBSOCKET_CONTINUATION_FRAME_HANDLER, new WebsocketContinuationFrameHandler());
        channel.pipeline().addAfter(Pipelines.WEBSOCKET_BINARY_FRAME_HANDLER, Pipelines.WEBSOCKET_TEXT_FRAME_HANDLER, new WebsocketTextFrameHandler());
        channel.pipeline().addAfter(Pipelines.WEBSOCKET_TEXT_FRAME_HANDLER, Pipelines.MQTT_WEBSOCKET_ENCODER, new MqttWebsocketEncoder());
    }

    @VisibleForTesting
    String getSubProtocols() {
        return Joiner.on(",").join(this.listener.getSubprotocols());
    }
}
