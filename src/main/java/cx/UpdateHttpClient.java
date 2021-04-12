package cx;

import ca1.Update;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.update.entity.UpdateRequest;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;

public class UpdateHttpClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateHttpClient.class);
    static final String CODEC = "codec";
    static final String INFLATER = "inflater";
    static final String CHUNK_AGGREGATOR = "chunk_aggregator";
    static final String UPDATE_RESPONSE_HANDLER = "update_responseHandler";
    static final String UPDATE_ERROR_HANDLER = "update_errorHandler";
    private final Provider<ObjectMapper> objectMapperProvider;
    private final URI updateURI;
    private final UpdateResponseHandler updateResponseHandler;
    private final Provider<EventLoopGroup> childEventLoopProvider;

    @Inject
    UpdateHttpClient(@Update Provider<ObjectMapper> objectMapperProvider,
                     @Update URI updateURI,
                     UpdateResponseHandler updateResponseHandler,
                     @Named("ChildEventLoop") Provider<EventLoopGroup> childEventLoopProvider) {
        this.objectMapperProvider = objectMapperProvider;
        this.updateURI = updateURI;
        this.updateResponseHandler = updateResponseHandler;
        this.childEventLoopProvider = childEventLoopProvider;
    }

    public ChannelFuture checkForUpdate(UpdateRequest request) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(this.childEventLoopProvider.get());
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.handler(new ChannelInitializer<SocketChannel>() {

            @Override
            protected void initChannel(SocketChannel channel) throws Exception {
                channel.pipeline().addLast(CODEC, new HttpClientCodec());
                channel.pipeline().addLast(INFLATER, new HttpContentDecompressor());
                channel.pipeline().addLast(CHUNK_AGGREGATOR, new HttpObjectAggregator(1048576));
                channel.pipeline().addLast(UPDATE_RESPONSE_HANDLER, updateResponseHandler);
                channel.pipeline().addLast(UPDATE_ERROR_HANDLER, new UpdateErrorHandler());
            }
        });
        ChannelFuture channelFuture = bootstrap.connect(new InetSocketAddress(this.updateURI.getHost(), this.updateURI.getPort()));
        channelFuture.addListener(new ChannelFutureListener() {

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                Channel channel = future.channel();
                if (!future.isSuccess()) {
                    LOGGER.debug("Checking for updates was not successful");
                    return;
                }
                try {
                    byte[] requestBytes = objectMapperProvider.get().writeValueAsBytes(request);
                    ByteBuf requestByteBuf = Unpooled.buffer(requestBytes.length);
                    requestByteBuf.writeBytes(requestBytes);
                    DefaultFullHttpRequest fullHttpRequest = createFullHttpRequest(requestByteBuf);
                    fullHttpRequest.headers().set("Content-Length", requestBytes.length);
                    channel.writeAndFlush(fullHttpRequest, channel.voidPromise());
                } catch (IOException e) {
                    LOGGER.debug("Could not create update request", e);
                }
            }
        });
        return channelFuture.channel().closeFuture();
    }

    private DefaultFullHttpRequest createFullHttpRequest(ByteBuf request) {
        DefaultFullHttpRequest fullHttpRequest = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, HttpMethod.POST, this.updateURI.getRawPath(), request);
        fullHttpRequest.headers().set("Host", this.updateURI.getHost()).set("Connection", "keep-alive").set("Accept-Encoding", "gzip");
        return fullHttpRequest;
    }
}
