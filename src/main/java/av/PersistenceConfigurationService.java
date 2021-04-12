package av;

import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.services.configuration.ValueChangedCallback;

public interface PersistenceConfigurationService {
    PersistenceMode getRetainedMessagesMode();

    void setRetainedMessagesMode(@NotNull PersistenceMode mode);

    PersistenceConfig getRetainedMessagesConfig();

    void setRetainedMessagesConfig(@NotNull PersistenceConfig config);

    PersistenceMode getClientSessionGeneralMode();

    void setClientSessionGeneralMode(@NotNull PersistenceMode mode);

    PersistenceConfig getClientSessionGeneralConfig();

    void setClientSessionGeneralConfig(@NotNull PersistenceConfig config);

    PersistenceMode getClientSessionSubscriptionMode();

    void setClientSessionSubscriptionMode(@NotNull PersistenceMode mode);

    PersistenceConfig getClientSessionSubscriptionConfig();

    void setClientSessionSubscriptionConfig(@NotNull PersistenceConfig config);

    PersistenceMode getClientSessionQueuedMessagesMode();

    void setClientSessionQueuedMessagesMode(@NotNull PersistenceMode mode);

    long getMaxQueuedMessages();

    void setMaxQueuedMessages(long maxQueuedMessages);

    void register(@NotNull ValueChangedCallback<Long> callback);

    PersistenceConfig getClientSessionQueuedMessagesConfig();

    void setClientSessionQueuedMessagesConfig(@NotNull PersistenceConfig config);

    QueuedMessagesStrategy getQueuedMessagesStrategy();

    void setQueuedMessagesStrategy(@NotNull QueuedMessagesStrategy strategy);

    PersistenceMode getMessageFlowIncomingMode();

    void setMessageFlowIncomingMode(@NotNull PersistenceMode mode);

    PersistenceConfig getMessageFlowIncomingConfig();

    void setMessageFlowIncomingConfig(@NotNull PersistenceConfig config);

    PersistenceMode getMessageFlowOutgoingMode();

    void setMessageFlowOutgoingMode(@NotNull PersistenceMode mode);

    PersistenceConfig getMessageFlowOutgoingConfig();

    void setMessageFlowOutgoingConfig(@NotNull PersistenceConfig config);

    enum QueuedMessagesStrategy {
        DISCARD_OLDEST,
        DISCARD
    }

    enum PersistenceMode {
        IN_MEMORY,
        FILE
    }
}
