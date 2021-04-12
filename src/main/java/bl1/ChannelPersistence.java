package bl1;

import com.hivemq.spi.annotations.Nullable;
import io.netty.channel.Channel;

import java.util.Set;

public interface ChannelPersistence {
    @Nullable
    Channel getChannel(String clientId);

    void persist(String clientId, Channel channel);

    @Nullable
    Channel remove(String clientId);

    long size();

    boolean isEmpty();

    void clear();

    boolean isClientConnected(String clientId);

    Set<String> getAllClients();

    Set<String> getConnectedClients();
}
