package bt;

import c.BaseModule;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;

public class PluginCallbackModule extends BaseModule<PluginCallbackModule> {
    public PluginCallbackModule() {
        super(PluginCallbackModule.class);
    }

    protected void configure() {
        DefaultEventExecutorGroup eventExecutorGroup = new DefaultEventExecutorGroup(16,
                new ThreadFactoryBuilder().setNameFormat("hivemq-event-executor-%d").build());
        bind(EventExecutorGroup.class).toInstance(eventExecutorGroup);
    }
}
