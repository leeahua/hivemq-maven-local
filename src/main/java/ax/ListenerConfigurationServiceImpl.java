package ax;

import com.google.common.collect.ImmutableList;
import com.hivemq.spi.services.configuration.entity.Listener;
import com.hivemq.spi.services.configuration.entity.TcpListener;
import com.hivemq.spi.services.configuration.entity.TlsTcpListener;
import com.hivemq.spi.services.configuration.entity.TlsWebsocketListener;
import com.hivemq.spi.services.configuration.entity.WebsocketListener;
import com.hivemq.spi.services.configuration.listener.ListenerConfigurationService;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class ListenerConfigurationServiceImpl implements ListenerConfigurationService {
    private final List<Listener> listeners = new ArrayList<>();

    public synchronized <T extends Listener> void addListener(T listener) {
        if (listener.getClass().equals(TcpListener.class) ||
                listener.getClass().equals(TlsTcpListener.class) ||
                listener.getClass().equals(WebsocketListener.class) ||
                listener.getClass().equals(TlsWebsocketListener.class)) {
            this.listeners.add(listener);
            return;
        }
        throw new IllegalArgumentException(listener.getClass().getName() + " is not a valid listener type");
    }

    public synchronized List<Listener> getListeners() {
        return ImmutableList.copyOf(this.listeners);
    }

    public synchronized List<TcpListener> getTcpListeners() {
        return getListenersByType(TcpListener.class);
    }

    public synchronized List<TlsTcpListener> getTlsTcpListeners() {
        return getListenersByType(TlsTcpListener.class);
    }

    public synchronized List<WebsocketListener> getWebsocketListeners() {
        return getListenersByType(WebsocketListener.class);
    }

    public synchronized List<TlsWebsocketListener> getTlsWebsocketListeners() {
        return getListenersByType(TlsWebsocketListener.class);
    }

    public synchronized void clear() {
        this.listeners.clear();
    }

    private <T extends Listener> List<T> getListenersByType(Class<T> type) {
        ImmutableList.Builder<T> builder = ImmutableList.builder();
        this.listeners.stream()
                .filter(listener -> listener.getClass().equals(type))
                .map(listener -> (T) listener)
                .forEach(builder::add);
        return builder.build();
    }
}
