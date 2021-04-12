package g;

import c.BaseModule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.name.Names;
import ct.SslContextStore;
import ct.SslFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ThreadFactory;

public class NettyModule extends BaseModule<NettyModule> {
    public NettyModule() {
        super(NettyModule.class);
    }

    protected void configure() {
        bind(ChannelGroup.class).toInstance(new DefaultChannelGroup(GlobalEventExecutor.INSTANCE));
        bind(SslFactory.class).toInstance(new SslFactory(new SslContextStore()));
        ThreadFactory parentThreadFactory = new ThreadFactoryBuilder().setNameFormat("hivemq-event-loop-parent-%d").build();
        ThreadFactory childThreadFactory = new ThreadFactoryBuilder().setNameFormat("hivemq-event-loop-child-%d").build();
        NioEventLoopGroup parentEventLoop = new NioEventLoopGroup(1, parentThreadFactory);
        NioEventLoopGroup childEventLoop = new NioEventLoopGroup(0, childThreadFactory);
        bind(EventLoopGroup.class).annotatedWith(Names.named("ParentEventLoop")).toInstance(parentEventLoop);
        bind(EventLoopGroup.class).annotatedWith(Names.named("ChildEventLoop")).toInstance(childEventLoop);
    }
}
