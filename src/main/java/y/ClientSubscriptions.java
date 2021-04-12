package y;

import com.google.common.collect.ImmutableSet;
import com.hivemq.spi.message.Topic;

public class ClientSubscriptions {
    private final ImmutableSet<Topic> subscriptions;

    public ClientSubscriptions(ImmutableSet<Topic> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public ImmutableSet<Topic> getSubscriptions() {
        return subscriptions;
    }
}
