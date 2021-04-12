package cv;

import c.BaseModule;
import io.netty.handler.traffic.GlobalTrafficShapingHandler;

import javax.inject.Singleton;

public class TrafficShapingModule extends BaseModule<TrafficShapingModule> {
    public TrafficShapingModule() {
        super(TrafficShapingModule.class);
    }

    protected void configure() {
        bind(GlobalTrafficShapingHandler.class).toProvider(GlobalTrafficShapingHandlerProvider.class).in(Singleton.class);
    }
}
