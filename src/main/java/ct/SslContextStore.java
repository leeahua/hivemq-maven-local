package ct;

import com.hivemq.spi.services.configuration.entity.Tls;
import io.netty.handler.ssl.SslContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SslContextStore {
    private final Map<Tls, SslContext> store = new ConcurrentHashMap<>();

    public boolean contains(Tls tls) {
        return this.store.containsKey(tls);
    }

    public boolean contains(SslContext context) {
        return this.store.containsValue(context);
    }

    public SslContext get(Tls tls) {
        return this.store.get(tls);
    }

    public void put(Tls tls, SslContext context) {
        this.store.put(tls, context);
    }

    public void remove(Tls tls) {
        this.store.remove(tls);
    }
}
