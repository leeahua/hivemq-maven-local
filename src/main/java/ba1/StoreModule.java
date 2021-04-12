package ba1;

import ai.TopicTreeClusterPersistenceProvider;
import ai.TopicTreeSinglePersistenceProvider;
import bb1.ClientSessionLocalPersistenceProvider;
import bb1.ClientSessionSubscriptionsLocalPersistenceProvider;
import bb1.IncomingMessageFlowLocalPersistenceProvider;
import bb1.OutgoingMessageFlowLocalPersistenceProvider;
import bb1.QueuedMessagesLocalPersistenceProvider;
import bb1.RetainedMessagesLocalPersistenceProvider;
import bc1.ClientSessionLocalPersistence;
import bc1.ClientSessionSubscriptionsLocalPersistence;
import bc1.IncomingMessageFlowLocalPersistence;
import bc1.LWTLocalPersistence;
import bc1.OutgoingMessageFlowLocalPersistence;
import bc1.QueuedMessagesLocalPersistence;
import be1.LWTLocalPersistenceImpl;
import bg1.OutgoingMessageFlowLocalXodusPersistence;
import bg1.RetainedMessagesLocalXodusPersistence;
import bi1.ClientSessionLocalXodusPersistence;
import bi1.ClientSessionSubscriptionsLocalXodusPersistence;
import bj1.QueuedMessagesLocalFilePersistence;
import bl1.ChannelPersistence;
import bl1.ChannelPersistenceImpl;
import bl1.LWTPersistence;
import bl1.LWTPersistenceImpl;
import bm1.ClientSessionClusterPersistenceProvider;
import bm1.ClientSessionSinglePersistence;
import bm1.ClientSessionSinglePersistenceProvider;
import bm1.ClientSessionSubscriptionClusterPersistenceProvider;
import bm1.ClientSessionSubscriptionsSinglePersistence;
import bm1.ClientSessionSubscriptionsPersistenceProvider;
import bm1.QueuedMessagePersistenceProvider;
import bm1.QueuedMessagesClusterPersistenceProvider;
import bm1.QueuedMessagesSinglePersistence;
import bn1.IncomingMessageFlowPersistence;
import bn1.IncomingMessageFlowPersistenceImpl;
import bn1.OutgoingMessageFlowClusterPersistence;
import bn1.OutgoingMessageFlowClusterPersistenceProvider;
import bn1.OutgoingMessageFlowSinglePersistence;
import bn1.OutgoingMessageFlowPersistenceProvider;
import by.TopicTreeClusterPersistence;
import by.TopicTreeSinglePersistence;
import c.BaseModule;
import com.google.inject.Injector;
import com.hivemq.spi.annotations.Nullable;
import d.CacheScoped;
import v.ClientSessionClusterPersistence;
import w.QueuedMessagesClusterPersistence;
import x.RetainedMessageClusterPersistenceProvider;
import x.RetainedMessagesClusterPersistence;
import x.RetainedMessagesLocalPersistence;
import x.RetainedMessagesSinglePersistence;
import x.RetainedMessagesPersistenceProvider;
import y.ClientSessionSubscriptionsClusterPersistence;

import javax.inject.Singleton;


class StoreModule extends BaseModule<StoreModule> {
    private final Injector injector;

    public StoreModule(@Nullable Injector injector) {
        super(StoreModule.class);
        this.injector = injector;
    }

    protected void configure() {
        bind(LWTLocalPersistence.class).to(LWTLocalPersistenceImpl.class);
        bind(LWTPersistence.class).to(LWTPersistenceImpl.class).in(CacheScoped.class);

        bind(RetainedMessagesSinglePersistence.class).toProvider(RetainedMessagesPersistenceProvider.class).in(CacheScoped.class);
        bind(RetainedMessagesClusterPersistence.class).toProvider(RetainedMessageClusterPersistenceProvider.class).in(CacheScoped.class);
        bind(RetainedMessagesLocalPersistence.class, RetainedMessagesLocalXodusPersistence.class, RetainedMessagesLocalPersistenceProvider.class);

        bind(ChannelPersistence.class).to(ChannelPersistenceImpl.class).in(Singleton.class);

        bind(ClientSessionClusterPersistence.class).toProvider(ClientSessionClusterPersistenceProvider.class).in(CacheScoped.class);
        bind(ClientSessionSinglePersistence.class).toProvider(ClientSessionSinglePersistenceProvider.class).in(CacheScoped.class);
        bind(ClientSessionLocalPersistence.class, ClientSessionLocalXodusPersistence.class, ClientSessionLocalPersistenceProvider.class);

        bind(ClientSessionSubscriptionsSinglePersistence.class).toProvider(ClientSessionSubscriptionsPersistenceProvider.class).in(CacheScoped.class);
        bind(ClientSessionSubscriptionsClusterPersistence.class).toProvider(ClientSessionSubscriptionClusterPersistenceProvider.class).in(CacheScoped.class);
        bind(ClientSessionSubscriptionsLocalPersistence.class, ClientSessionSubscriptionsLocalXodusPersistence.class, ClientSessionSubscriptionsLocalPersistenceProvider.class);

        bind(QueuedMessagesSinglePersistence.class).toProvider(QueuedMessagePersistenceProvider.class).in(CacheScoped.class);
        bind(QueuedMessagesClusterPersistence.class).toProvider(QueuedMessagesClusterPersistenceProvider.class).in(CacheScoped.class);
        bind(QueuedMessagesLocalPersistence.class, QueuedMessagesLocalFilePersistence.class, QueuedMessagesLocalPersistenceProvider.class);

        bind(TopicTreeSinglePersistence.class).toProvider(TopicTreeSinglePersistenceProvider.class).in(Singleton.class);
        bind(TopicTreeClusterPersistence.class).toProvider(TopicTreeClusterPersistenceProvider.class).in(Singleton.class);

        bind(IncomingMessageFlowPersistence.class).to(IncomingMessageFlowPersistenceImpl.class);
        bind(IncomingMessageFlowLocalPersistence.class).toProvider(IncomingMessageFlowLocalPersistenceProvider.class).in(CacheScoped.class);

        bind(OutgoingMessageFlowSinglePersistence.class).toProvider(OutgoingMessageFlowPersistenceProvider.class).in(CacheScoped.class);
        bind(OutgoingMessageFlowClusterPersistence.class).toProvider(OutgoingMessageFlowClusterPersistenceProvider.class).in(CacheScoped.class);
        bind(OutgoingMessageFlowLocalPersistence.class, OutgoingMessageFlowLocalXodusPersistence.class, OutgoingMessageFlowLocalPersistenceProvider.class);
    }

    private void bind(Class interfaceType, Class implementsType, Class provider) {
        if (this.injector == null) {
            bind(interfaceType).toProvider(provider).in(Singleton.class);
            return;
        }
        Object instance = this.injector.getInstance(implementsType);
        if (instance != null) {
            bind(implementsType).toInstance(instance);
            bind(interfaceType).toInstance(instance);
        } else {
            bind(interfaceType).toProvider(provider).in(Singleton.class);
        }
    }
}
