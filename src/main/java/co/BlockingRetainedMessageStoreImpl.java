package co;

import com.google.inject.Inject;
import com.hivemq.spi.message.RetainedMessage;
import com.hivemq.spi.services.AsyncRetainedMessageStore;
import com.hivemq.spi.services.BlockingRetainedMessageStore;
import d.CacheScoped;

import java.util.Set;

@CacheScoped
public class BlockingRetainedMessageStoreImpl implements BlockingRetainedMessageStore {
    private final AsyncRetainedMessageStore asyncRetainedMessageStore;

    @Inject
    public BlockingRetainedMessageStoreImpl(AsyncRetainedMessageStore asyncRetainedMessageStore) {
        this.asyncRetainedMessageStore = asyncRetainedMessageStore;
    }

    public Set<RetainedMessage> getLocalRetainedMessages() {
        try {
            return this.asyncRetainedMessageStore.getLocalRetainedMessages().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Long localSize() {
        try {
            return this.asyncRetainedMessageStore.localSize().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean containsLocally(String topic) {
        try {
            return this.asyncRetainedMessageStore.containsLocally(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Set<RetainedMessage> getRetainedMessages() {
        try {
            return this.asyncRetainedMessageStore.getRetainedMessages().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public RetainedMessage getRetainedMessage(String topic) {
        try {
            return this.asyncRetainedMessageStore.getRetainedMessage(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(String topic) {
        try {
            this.asyncRetainedMessageStore.remove(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void clear() {
        try {
            this.asyncRetainedMessageStore.clear().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void addOrReplace(RetainedMessage retainedMessage) {
        try {
            this.asyncRetainedMessageStore.addOrReplace(retainedMessage).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean contains(String topic) {
        try {
            return this.asyncRetainedMessageStore.contains(topic).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public long size() {
        try {
            return this.asyncRetainedMessageStore.size().get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
