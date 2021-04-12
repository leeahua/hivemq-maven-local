package r;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;

import javax.inject.Provider;

public class ClusterBootstrapProvider implements Provider<Bootstrap> {

    @Override
    public Bootstrap get() {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new LocalEventLoopGroup())
                .channel(LocalChannel.class)
                .handler(new ChannelInitializer<LocalChannel>() {

                    @Override
                    protected void initChannel(LocalChannel ch) throws Exception {

                    }
                });
        return bootstrap;
    }
}
