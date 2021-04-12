package b;

import ap.ShutdownRegistry;
import av.HiveMQConfigurationService;
import av.Internals;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.SettableFuture;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.configuration.entity.Listener;
import com.hivemq.spi.services.configuration.entity.TcpListener;
import com.hivemq.spi.services.configuration.entity.TlsTcpListener;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import com.hivemq.spi.services.configuration.listener.ListenerConfigurationService;
import e.ChannelInitializerFactory;
import e.ChannelPipelineDependencies;
import f.AbstractChannelInitializer;
import i.ClusterConfigurationService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class NettyServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyServer.class);
    private final ShutdownRegistry shutdownRegistry;
    private final ListenerConfigurationService listenerConfigurationService;
    private final EventLoopGroup parentEventLoop;
    private final EventLoopGroup childEventLoop;
    private final ChannelInitializerFactory factory;
    private final ChannelPipelineDependencies dependencies;
    private final ClusterConfigurationService clusterConfigurationService;
    private final EventExecutorGroup eventExecutorGroup;
    private final HiveMQConfigurationService hiveMQConfigurationService;

    @Inject
    NettyServer(ShutdownRegistry shutdownRegistry,
                ListenerConfigurationService listenerConfigurationService,
                ChannelInitializerFactory factory,
                ChannelPipelineDependencies dependencies,
                ClusterConfigurationService clusterConfigurationService,
                EventExecutorGroup eventExecutorGroup,
                @Named("ParentEventLoop") EventLoopGroup parentEventLoop,
                @Named("ChildEventLoop") EventLoopGroup childEventLoop,
                HiveMQConfigurationService hiveMQConfigurationService) {
        this.shutdownRegistry = shutdownRegistry;
        this.listenerConfigurationService = listenerConfigurationService;
        this.factory = factory;
        this.dependencies = dependencies;
        this.clusterConfigurationService = clusterConfigurationService;
        this.eventExecutorGroup = eventExecutorGroup;
        this.parentEventLoop = parentEventLoop;
        this.childEventLoop = childEventLoop;
        this.hiveMQConfigurationService = hiveMQConfigurationService;
    }

    public ListenableFuture<List<ListenerStartResult>> startListeners(List<Listener> listeners) {
        Preconditions.checkNotNull(listeners, "Listeners must not be null");
        Preconditions.checkArgument(listeners.size() > 0, "There must be at least one listener");
        listeners.forEach(this.listenerConfigurationService::addListener);
        return startListeners();
    }

    public ListenableFuture<List<ListenerStartResult>> startListeners() {
        this.shutdownRegistry.register(new NettyShutdown(this.childEventLoop, this.parentEventLoop, this.hiveMQConfigurationService.internalConfiguration().getInt(Internals.EVENT_LOOP_GROUP_SHUTDOWN_TIMEOUT)));
        List<ListenerStartFuture> startFutures = new ArrayList<>();
        addDefaultListenerWhenListenersIsEmpty();
        startFutures.addAll(startTcpListeners(this.listenerConfigurationService.getTcpListeners()));
        startFutures.addAll(startTlsTcpListeners(this.listenerConfigurationService.getTlsTcpListeners()));
        startFutures.addAll(startWebsocketListeners(this.listenerConfigurationService.getWebsocketListeners()));
        startFutures.addAll(startTlsWebsocketListeners(this.listenerConfigurationService.getTlsWebsocketListeners()));
        return getStartResult(startFutures);
    }

    private void addDefaultListenerWhenListenersIsEmpty() {
        if (this.listenerConfigurationService.getListeners().isEmpty()) {
            this.listenerConfigurationService.addListener(new TcpListener(1883, "0.0.0.0"));
        }
    }

    public ChannelFuture start() {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(this.parentEventLoop, this.childEventLoop)
                .channel(LocalServerChannel.class)
                .childHandler(new AbstractChannelInitializer(dependencies, eventExecutorGroup) {

                    @Override
                    protected void initTransportChannel(Channel channel) {

                    }
                })
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, Boolean.TRUE)
                .childOption(ChannelOption.TCP_NODELAY, Boolean.TRUE)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return serverBootstrap.bind(this.clusterConfigurationService.getLocalAddress());
    }

    private List<ListenerStartFuture> startTcpListeners(List<TcpListener> listeners) {
        LOGGER.trace("Checking TCP listeners");
        ImmutableList.Builder<ListenerStartFuture> builder = ImmutableList.builder();
        listeners.forEach(listener -> {
            ServerBootstrap serverBootstrap = createServerBootstrap(this.parentEventLoop, this.childEventLoop, listener);
            String bindAddress = listener.getBindAddress();
            int port = listener.getPort();
            LOGGER.info("Starting TCP listener on address {} and port {}", bindAddress, port);
            ChannelFuture channelFuture = serverBootstrap.bind(createSocketAddress(bindAddress, port));
            builder.add(new ListenerStartFuture(listener, channelFuture));
        });
        return builder.build();
    }

    private List<ListenerStartFuture> startTlsTcpListeners(List<TlsTcpListener> listeners) {
        LOGGER.trace("Checking TLS TCP listeners");
        ImmutableList.Builder<ListenerStartFuture> builder = ImmutableList.builder();
        listeners.forEach(listener -> {
            ServerBootstrap serverBootstrap = createServerBootstrap(this.parentEventLoop, this.childEventLoop, listener);
            String bindAddress = listener.getBindAddress();
            int port = listener.getPort();
            LOGGER.info("Starting TLS TCP listener on address {} and port {}", bindAddress, port);
            ChannelFuture channelFuture = serverBootstrap.bind(bindAddress, port);
            builder.add(new ListenerStartFuture(listener, channelFuture));
        });
        return builder.build();
    }

    private List<ListenerStartFuture> startWebsocketListeners(List<WebsocketListener> listeners) {
        LOGGER.trace("Checking Websocket listeners");
        ImmutableList.Builder<ListenerStartFuture> builder = ImmutableList.builder();
        listeners.forEach(listener -> {
            ServerBootstrap serverBootstrap = createServerBootstrap(this.parentEventLoop, this.childEventLoop, listener);
            String bindAddress = listener.getBindAddress();
            int port = listener.getPort();
            LOGGER.info("Starting Websocket listener on address {} and port {}", bindAddress, port);
            ChannelFuture channelFuture = serverBootstrap.bind(bindAddress, port);
            builder.add(new ListenerStartFuture(listener, channelFuture));
        });
        return builder.build();
    }

    private List<ListenerStartFuture> startTlsWebsocketListeners(List<TlsWebsocketListener> listeners) {
        LOGGER.trace("Checking Websocket TLS listeners");
        ImmutableList.Builder<ListenerStartFuture> builder = ImmutableList.builder();
        listeners.forEach(listener -> {
            ServerBootstrap serverBootstrap = createServerBootstrap(this.parentEventLoop, this.childEventLoop, listener);
            String bindAddress = listener.getBindAddress();
            int port = listener.getPort();
            LOGGER.info("Starting Websocket TLS listener on address {} and port {}", bindAddress, port);
            ChannelFuture channelFuture = serverBootstrap.bind(bindAddress, port);
            builder.add(new ListenerStartFuture(listener, channelFuture));
        });
        return builder.build();
    }

    private InetSocketAddress createSocketAddress(String address, int port) {
        InetAddress inetAddress;
        try {
            inetAddress = InetAddresses.forString(address);
        } catch (IllegalArgumentException e) {
            LOGGER.debug("Trying to find out the IP for {} via DNS lookup", address);
            try {
                inetAddress = InetAddress.getByName(address);
            } catch (UnknownHostException e1) {
                LOGGER.error("Could not determine IP for hostname {}, unable to bind to it", address);
                throw new UnrecoverableException(false);
            }
        }
        return new InetSocketAddress(inetAddress, port);
    }

    private ListenableFuture<List<ListenerStartResult>> getStartResult(
            List<ListenerStartFuture> startFutures) {
        List<ListenableFuture<ListenerStartResult>> futures = Lists.transform(startFutures, startFuture -> {
            SettableFuture<ListenerStartResult> settableFuture = SettableFuture.create();
            startFuture.getFuture().addListener(createStartFutureListener(settableFuture, startFuture));
            return settableFuture;
        });
        return Futures.allAsList(futures);
    }


    private ChannelFutureListener createStartFutureListener(
            SettableFuture<ListenerStartResult> settableFuture, ListenerStartFuture startFuture) {
        return new StartFutureListener(startFuture, settableFuture);
    }


    private ServerBootstrap createServerBootstrap(EventLoopGroup parentEventLoop,
                                                  EventLoopGroup childEventLoop,
                                                  Listener listener) {
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap
                .group(parentEventLoop, childEventLoop)
                .channel(NioServerSocketChannel.class)
                .childHandler(this.factory.create(listener))
                .option(ChannelOption.SO_BACKLOG, 128)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_REUSEADDR, true)
                .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
        return serverBootstrap;
    }

    private static class StartFutureListener implements ChannelFutureListener {
        private final ListenerStartFuture listenerStartFuture;
        private final SettableFuture<ListenerStartResult> settableFuture;

        public StartFutureListener(@NotNull ListenerStartFuture listenerStartFuture, @NotNull SettableFuture<ListenerStartResult> settableFuture) {
            this.listenerStartFuture = listenerStartFuture;
            this.settableFuture = settableFuture;
        }

        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
            Listener listener = listenerStartFuture.getListener();
            int port = listener.getPort();
            if (future.isSuccess()) {
                this.settableFuture.set(ListenerStartResult.of(port, listener));
            } else {
                this.settableFuture.set(ListenerStartResult.of(port, listener, future.cause()));
            }
        }
    }
}
