package r;

import a1.ClusterRequestDispatcher;
import ac.SerializationService;
import ac.SerializationServiceImpl;
import aj.ClusterFutures;
import c.BaseModule;
import com.esotericsoftware.kryo.Kryo;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import d.CacheScoped;
import i.PublishDispatcher;
import i.PublishDispatcherImpl;
import io.netty.bootstrap.Bootstrap;
import net.openhft.hashing.LongHashFunction;
import q.ConsistentHashingRing;
import s.Cluster;
import s.Rpc;
import s.Minority;
import s.Primary;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;

public class ClusterModule extends BaseModule<ClusterModule> {
    public ClusterModule() {
        super(ClusterModule.class);
    }

    protected void configure() {
        bind(ExecutorService.class).annotatedWith(Cluster.class).toProvider(ClusterExecutorProvider.class).in(CacheScoped.class);
        bind(ListeningExecutorService.class).annotatedWith(Cluster.class).toProvider(ClusterExecutorProvider.class).in(CacheScoped.class);
        bind(ExecutorService.class).annotatedWith(Rpc.class).toProvider(RpcExecutorServiceProvider.class).in(CacheScoped.class);
        bind(ListeningExecutorService.class).annotatedWith(Rpc.class).toProvider(RpcExecutorServiceProvider.class).in(CacheScoped.class);
        bind(ScheduledExecutorService.class).annotatedWith(Cluster.class).toProvider(ClusterSchedulerProvider.class).in(CacheScoped.class);
        bind(ListeningScheduledExecutorService.class).annotatedWith(Cluster.class).toProvider(ClusterSchedulerProvider.class).in(CacheScoped.class);
        bind(SerializationService.class).to(SerializationServiceImpl.class).in(CacheScoped.class);
        bind(Kryo.class).toProvider(KryoProvider.class);
        bind(ClusterRequestDispatcher.class).toProvider(ClusterRequestDispatcherProvider.class).in(CacheScoped.class);
        bind(Bootstrap.class).toProvider(ClusterBootstrapProvider.class).in(CacheScoped.class);
        bind(ConsistentHashingRing.class).annotatedWith(Minority.class).toInstance(new ConsistentHashingRing("consistent hashing with joining nodes", LongHashFunction.xx_r39()));
        bind(ConsistentHashingRing.class).annotatedWith(Primary.class).toInstance(new ConsistentHashingRing("consistent hashing without joining nodes", LongHashFunction.xx_r39()));
        bind(PublishDispatcher.class).to(PublishDispatcherImpl.class).in(CacheScoped.class);
        requestStaticInjection(ClusterFutures.class);
    }
}
