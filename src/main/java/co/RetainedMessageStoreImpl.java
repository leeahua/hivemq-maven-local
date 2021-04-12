package co;

import com.google.inject.Inject;
import com.hivemq.spi.message.RetainedMessage;
import com.hivemq.spi.services.BlockingRetainedMessageStore;
import com.hivemq.spi.services.RetainedMessageStore;
import d.CacheScoped;

import java.util.Optional;
import java.util.Set;

@CacheScoped
public class RetainedMessageStoreImpl
        implements RetainedMessageStore {
    private final BlockingRetainedMessageStore blockingRetainedMessageStore;

    @Inject
    public RetainedMessageStoreImpl(BlockingRetainedMessageStore blockingRetainedMessageStore) {
        this.blockingRetainedMessageStore = blockingRetainedMessageStore;
    }

    public Set<RetainedMessage> getRetainedMessages() {
        return this.blockingRetainedMessageStore.getRetainedMessages();
    }

    public Optional<RetainedMessage> getRetainedMessage(String topic) {
        return Optional.ofNullable(this.blockingRetainedMessageStore.getRetainedMessage(topic));
    }

    public void remove(String topic) {
        this.blockingRetainedMessageStore.remove(topic);
    }

    public void remove(RetainedMessage retainedMessage) {
        this.blockingRetainedMessageStore.remove(retainedMessage.getTopic());
    }

    public void clear() {
        this.blockingRetainedMessageStore.clear();
    }

    public void addOrReplace(RetainedMessage retainedMessage) {
        this.blockingRetainedMessageStore.addOrReplace(retainedMessage);
    }

    public boolean contains(RetainedMessage retainedMessage) {
        return this.blockingRetainedMessageStore.contains(retainedMessage.getTopic());
    }

    public int size() {
        return (int) this.blockingRetainedMessageStore.size();
    }
}
