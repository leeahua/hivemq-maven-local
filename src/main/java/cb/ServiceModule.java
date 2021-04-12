package cb;

import bp1.CallbackRegistryImpl;
import c.BaseModule;
import ca.CallbackExecutor;
import cc.OnBrokerStopCallbacksShutdownRegister;
import cd.QuartzCallbackScheduler;
import co.AsyncMetricServiceImpl;
import co.AsyncRetainedMessageStoreImpl;
import co.AsyncSubscriptionStoreImpl;
import co.BlockingClientServiceImpl;
import co.BlockingMetricServiceImpl;
import co.BlockingRetainedMessageStoreImpl;
import co.BlockingSubscriptionStoreImpl;
import co.BridgeManagerServiceImpl;
import co.ClientServiceImpl;
import co.LogServiceImpl;
import co.PluginExecutorServiceImpl;
import co.PublishServiceImpl;
import co.RetainedMessageStoreImpl;
import co.SYSTopicServiceImpl;
import co.SharedSubscriptionServiceImpl;
import co.SubscriptionStoreImpl;
import co.InternalPublishService;
import com.google.inject.Provides;
import com.hivemq.spi.PluginModule;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.services.AsyncClientService;
import com.hivemq.spi.services.AsyncMetricService;
import com.hivemq.spi.services.AsyncRetainedMessageStore;
import com.hivemq.spi.services.AsyncSubscriptionStore;
import com.hivemq.spi.services.BlockingClientService;
import com.hivemq.spi.services.BlockingMetricService;
import com.hivemq.spi.services.BlockingRetainedMessageStore;
import com.hivemq.spi.services.BlockingSubscriptionStore;
import com.hivemq.spi.services.BridgeManagerService;
import com.hivemq.spi.services.ClientService;
import com.hivemq.spi.services.LogService;
import com.hivemq.spi.services.MetricService;
import com.hivemq.spi.services.PluginExecutorService;
import com.hivemq.spi.services.PublishService;
import com.hivemq.spi.services.RetainedMessageStore;
import com.hivemq.spi.services.SYSTopicService;
import com.hivemq.spi.services.SharedSubscriptionService;
import com.hivemq.spi.services.SubscriptionStore;
import com.hivemq.spi.services.rest.RESTService;
import cp.RestServiceImpl;
import d.CacheScoped;
import d.ScopeModule;
import org.quartz.Scheduler;
import org.quartz.SchedulerFactory;

import java.util.List;

public class ServiceModule extends BaseModule<ServiceModule> {
    final List<PluginModule> pluginModules;

    public ServiceModule(List<PluginModule> pluginModules) {
        super(ServiceModule.class);
        this.pluginModules = pluginModules;
    }

    protected void configure() {
        install(new ScopeModule());
        bind(CallbackRegistry.class).to(CallbackRegistryImpl.class);
        bind(QuartzCallbackScheduler.class);
        bind(Scheduler.class).toProvider(QuartzCallbackSchedulerProvider.class).in(CacheScoped.class);
        bind(SchedulerFactory.class).toProvider(QuartzCallbackSchedulerFactory.class).in(CacheScoped.class);
        asEagerSingleton(OnBrokerStopCallbacksShutdownRegister.class);
        bind(PluginExecutorService.class).to(PluginExecutorServiceImpl.class).in(CacheScoped.class);
        bind(RetainedMessageStore.class).to(RetainedMessageStoreImpl.class).in(CacheScoped.class);
        bind(BlockingRetainedMessageStore.class).to(BlockingRetainedMessageStoreImpl.class).in(CacheScoped.class);
        bind(AsyncRetainedMessageStore.class).to(AsyncRetainedMessageStoreImpl.class).in(CacheScoped.class);
        bind(PublishService.class).to(PublishServiceImpl.class).in(CacheScoped.class);
        bind(InternalPublishService.class).to(PublishServiceImpl.class).in(CacheScoped.class);
        bind(SYSTopicService.class).to(SYSTopicServiceImpl.class).in(CacheScoped.class);
        bind(MetricService.class).to(BlockingMetricServiceImpl.class).in(CacheScoped.class);
        bind(BlockingMetricService.class).to(BlockingMetricServiceImpl.class).in(CacheScoped.class);
        bind(AsyncMetricService.class).to(AsyncMetricServiceImpl.class).in(CacheScoped.class);
        bind(BlockingMetricService.class).to(BlockingMetricServiceImpl.class).in(CacheScoped.class);
        bind(SubscriptionStore.class).to(SubscriptionStoreImpl.class).in(CacheScoped.class);
        bind(AsyncSubscriptionStore.class).to(AsyncSubscriptionStoreImpl.class).in(CacheScoped.class);
        bind(BlockingSubscriptionStore.class).to(BlockingSubscriptionStoreImpl.class).in(CacheScoped.class);
        bind(LogService.class).to(LogServiceImpl.class).in(CacheScoped.class);
        bind(ClientService.class).to(ClientServiceImpl.class).in(CacheScoped.class);
        bind(AsyncClientService.class).to(co.AsyncClientServiceImpl.class).in(CacheScoped.class);
        bind(BlockingClientService.class).to(BlockingClientServiceImpl.class).in(CacheScoped.class);
        bind(RESTService.class).to(RestServiceImpl.class).in(CacheScoped.class);
        bind(BridgeManagerService.class).to(BridgeManagerServiceImpl.class).in(CacheScoped.class);
        bind(SharedSubscriptionService.class).to(SharedSubscriptionServiceImpl.class).in(CacheScoped.class);
        bind(CallbackExecutor.class).toProvider(CallbackExecutorProvider.class).in(CacheScoped.class);
    }

    @Provides
    public List<PluginModule> providePluginModules() {
        return this.pluginModules;
    }
}
