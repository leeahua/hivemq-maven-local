package ba1;

import by.TopicTreeBootstrap;
import bz.PersistenceShutdownRegister;
import c.BaseModule;
import com.google.inject.Injector;
import com.hivemq.spi.annotations.Nullable;
import u.PersistenceCleanupService;

public class PersistenceModule
        extends BaseModule<PersistenceModule> {
    private final Injector injector;

    public PersistenceModule(@Nullable Injector injector) {
        super(PersistenceModule.class);
        this.injector = injector;
    }

    protected void configure() {
        install(new StoreModule(this.injector));
        bind(PersistenceShutdownRegister.class).asEagerSingleton();
        bind(TopicTreeBootstrap.class).asEagerSingleton();
        bind(PersistenceCleanupService.class).asEagerSingleton();
    }
}
