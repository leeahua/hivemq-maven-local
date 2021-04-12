package c;

import ao.Preference;
import bx.PublishTopicMatcher;
import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.hivemq.spi.topic.TopicMatcher;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.RejectedExecutionException;
import java.util.prefs.Preferences;

public class InternalModule extends BaseModule<InternalModule> {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalModule.class);

    public InternalModule() {
        super(InternalModule.class);
    }

    protected void configure() {
        bind(TopicMatcher.class).to(PublishTopicMatcher.class);
        bind(Preferences.class).annotatedWith(Preference.class).toInstance(Preferences.userRoot().node("hivemq"));
    }

    @Provides
    @Singleton
    @Inject
    public EventBus provideEventBus(EventExecutorGroup eventExecutorGroup) {
        return new AsyncEventBus(eventExecutorGroup, (cause, context) -> {
            if (cause instanceof RejectedExecutionException) {
                LOGGER.debug("Exception in EventBus", cause);
            } else {
                LOGGER.warn("Exception in EventBus", cause);
            }
        });
    }
}
