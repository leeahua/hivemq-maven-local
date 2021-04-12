package cz;

import ca1.Update;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import cw.UpdateChecker;
import d.CacheScoped;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.prefs.Preferences;

public class UpdateModule extends AbstractModule {
    static final String UPDATE_URL = "http://update.hivemq.com:80/";

    protected void configure() {
        bind(Preferences.class).annotatedWith(Update.class).toInstance(Preferences.userRoot().node("hivemqUpdate"));
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("hivemq-update-check-%d").build();
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(threadFactory);
        bind(ScheduledExecutorService.class).annotatedWith(Update.class).toInstance(executorService);
        bind(UpdateChecker.class).in(Scopes.SINGLETON);
    }

    @Provides
    @Update
    @CacheScoped
    public ObjectMapper provideUpdateObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return objectMapper;
    }

    @Provides
    @Update
    public URI provideUpdateURI() {
        return URI.create(UPDATE_URL);
    }
}
