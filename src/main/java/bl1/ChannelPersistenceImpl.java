package bl1;

import com.hivemq.spi.annotations.Nullable;
import io.netty.channel.Channel;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ChannelPersistenceImpl implements ChannelPersistence {
    private final Map<String, Channel> channels = new ConcurrentHashMap<>();

    @Nullable
    public Channel getChannel(String clientId) {
        return this.channels.get(clientId);
    }

    public void persist(String clientId, Channel channel) {
        this.channels.put(clientId, channel);
    }

    @Nullable
    public Channel remove(String clientId) {
        return this.channels.remove(clientId);
    }

    public long size() {
        return this.channels.size();
    }

    public boolean isEmpty() {
        return this.channels.isEmpty();
    }

    public void clear() {
        this.channels.clear();
    }

    public boolean isClientConnected(String clientId) {
        return this.channels.containsKey(clientId);
    }

    public Set<String> getAllClients() {
        return this.channels.keySet();
    }

    public Set<String> getConnectedClients() {
        return this.channels.entrySet().stream()
                .filter(entry -> entry.getValue().isActive())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
}
