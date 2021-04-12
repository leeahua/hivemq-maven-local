package cc;

import cb1.PluginExceptionUtils;
import com.hivemq.spi.callback.events.broker.OnBrokerStart;
import com.hivemq.spi.callback.events.broker.OnBrokerStop;
import com.hivemq.spi.callback.exception.BrokerUnableToStartException;
import com.hivemq.spi.callback.registry.CallbackRegistry;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;

@Singleton
public class PluginBrokerCallbackHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginBrokerCallbackHandler.class);
    private final CallbackRegistry callbackRegistry;

    @Inject
    PluginBrokerCallbackHandler(CallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    public void onStart() {
        if (!this.callbackRegistry.isAvailable(OnBrokerStart.class)) {
            return;
        }
        List<OnBrokerStart> callbacks = this.callbackRegistry.getCallbacks(OnBrokerStart.class);
        try {
            Iterator<OnBrokerStart> iterator = callbacks.iterator();
            while (iterator.hasNext()) {
                OnBrokerStart callback = iterator.next();
                LOGGER.debug("Executing OnBrokerStart Callback {}", callback.getClass().getCanonicalName());
                callback.onBrokerStart();
            }
        } catch (BrokerUnableToStartException e) {
            onBrokerUnableToStart(e);
        } catch (Throwable e) {
            PluginExceptionUtils.logAndThrow(e);
        }
    }

    public void onStop() {
        if (!this.callbackRegistry.isAvailable(OnBrokerStop.class)) {
            return;
        }
        List<OnBrokerStop> callbacks = this.callbackRegistry.getCallbacks(OnBrokerStop.class);
        callbacks.forEach(callback -> {
            try {
                LOGGER.debug("Executing OnBrokerStop Callback {}", callback.getClass().getCanonicalName());
                callback.onBrokerStop();
            } catch (Throwable e) {
                LOGGER.error("An unhandled exception was thrown by a plugin. This is fatal because this kind of exceptions must be handled by the plugin.");
                LOGGER.error("Original Exception:", e);
            }
        });
    }

    private void onBrokerUnableToStart(BrokerUnableToStartException exception) {
        LOGGER.error("A plugin prohibited HiveMQ to start");
        if (exception.getMessage() != null) {
            LOGGER.error(exception.getMessage());
        }
        LOGGER.debug("Original exception: ", exception);
        throw new UnrecoverableException(false);
    }
}
