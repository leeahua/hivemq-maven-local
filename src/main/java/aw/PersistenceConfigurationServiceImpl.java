package aw;

import av.PersistenceConfig;
import av.PersistenceConfigurationService;
import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.services.configuration.ValueChangedCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

@ThreadSafe
@Singleton
public class PersistenceConfigurationServiceImpl
        implements PersistenceConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersistenceConfigurationServiceImpl.class);
    private static final String RETAINED_MESSAGES = "retained_messages";
    private static final String CLIENT_SESSION_GENERAL = "client_session_general";
    private static final String CLIENT_SESSION_SUBSCRIPTION = "client_session_subscription";
    private static final String CLIENT_SESSION_QUEUED_MESSAGES = "client_session_queued_messages";
    private static final String MESSAGE_FLOW_INCOMING = "message_flow_incoming";
    private static final String MESSAGE_FLOW_OUTGOING = "message_flow_outgoing";
    private final Map<String, PersistenceMode> persistenceModes = new ConcurrentHashMap<>(6);
    private final AtomicLong maxQueuedMessages = new AtomicLong(1000L);
    private volatile QueuedMessagesStrategy queuedMessagesStrategy = QueuedMessagesStrategy.DISCARD;
    private final List<ValueChangedCallback<Long>> maxQueuedMessagesChangedCallbacks = new CopyOnWriteArrayList<>();
    private final Map<String, PersistenceConfig> persistenceConfigs = new ConcurrentHashMap(6);

    public PersistenceMode getRetainedMessagesMode() {
        return this.persistenceModes.get(RETAINED_MESSAGES);
    }

    public void setRetainedMessagesMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be null");
        LOGGER.debug("Setting retained messages store mode to {}", mode.name());
        this.persistenceModes.put(RETAINED_MESSAGES, mode);
    }

    public PersistenceConfig getRetainedMessagesConfig() {
        return this.persistenceConfigs.get(RETAINED_MESSAGES);
    }

    public void setRetainedMessagesConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating retained messages persistence config");
        this.persistenceConfigs.put(RETAINED_MESSAGES, config);
    }

    public PersistenceMode getClientSessionGeneralMode() {
        return this.persistenceModes.get(CLIENT_SESSION_GENERAL);
    }

    public void setClientSessionGeneralMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be null");
        LOGGER.debug("Setting client session general store mode to {}", mode.name());
        this.persistenceModes.put(CLIENT_SESSION_GENERAL, mode);
    }

    public PersistenceConfig getClientSessionGeneralConfig() {
        return this.persistenceConfigs.get(CLIENT_SESSION_GENERAL);
    }

    public void setClientSessionGeneralConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating client session persistence config");
        this.persistenceConfigs.put(CLIENT_SESSION_GENERAL, config);
    }

    public PersistenceMode getClientSessionSubscriptionMode() {
        return this.persistenceModes.get(CLIENT_SESSION_SUBSCRIPTION);
    }

    public void setClientSessionSubscriptionMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be null");
        LOGGER.debug("Setting client session subscription store mode to {}", mode.name());
        this.persistenceModes.put(CLIENT_SESSION_SUBSCRIPTION, mode);
    }

    public PersistenceConfig getClientSessionSubscriptionConfig() {
        return this.persistenceConfigs.get(CLIENT_SESSION_SUBSCRIPTION);
    }

    public void setClientSessionSubscriptionConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating client session subscriptions persistence config");
        this.persistenceConfigs.put(CLIENT_SESSION_SUBSCRIPTION, config);
    }

    public PersistenceMode getClientSessionQueuedMessagesMode() {
        return this.persistenceModes.get(CLIENT_SESSION_QUEUED_MESSAGES);
    }

    public void setClientSessionQueuedMessagesMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be null");
        LOGGER.debug("Setting client session queued messages store mode to {}", mode.name());
        this.persistenceModes.put(CLIENT_SESSION_QUEUED_MESSAGES, mode);
    }

    public long getMaxQueuedMessages() {
        return this.maxQueuedMessages.get();
    }

    public void setMaxQueuedMessages(long maxQueuedMessages) {
        Preconditions.checkArgument(maxQueuedMessages >= 0L, "Max Queued Messages must be 0 or greater");
        LOGGER.debug("Setting max queued messages for each client to {}", Long.valueOf(maxQueuedMessages));
        if (maxQueuedMessages == 0L) {
            LOGGER.warn("Max Queued messages was set to 0. No messages will be queued for clients with a persistent session");
        }
        this.maxQueuedMessages.set(maxQueuedMessages);
        Iterator localIterator = this.maxQueuedMessagesChangedCallbacks.iterator();
        while (localIterator.hasNext()) {
            ValueChangedCallback localValueChangedCallback = (ValueChangedCallback) localIterator.next();
            localValueChangedCallback.valueChanged(Long.valueOf(maxQueuedMessages));
        }
    }

    public void register(@NotNull ValueChangedCallback<Long> callback) {
        Preconditions.checkNotNull(callback, "Callback must not be null");
        this.maxQueuedMessagesChangedCallbacks.add(callback);
    }

    public PersistenceConfig getClientSessionQueuedMessagesConfig() {
        return this.persistenceConfigs.get(CLIENT_SESSION_QUEUED_MESSAGES);
    }

    public void setClientSessionQueuedMessagesConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating client session queued message persistence config");
        this.persistenceConfigs.put(CLIENT_SESSION_QUEUED_MESSAGES, config);
    }

    public QueuedMessagesStrategy getQueuedMessagesStrategy() {
        return this.queuedMessagesStrategy;
    }

    public void setQueuedMessagesStrategy(@NotNull QueuedMessagesStrategy strategy) {
        Preconditions.checkNotNull(strategy, "Queued Messages strategy must not be null");
        LOGGER.debug("Setting queued messages strategy for each client to {}", strategy.name());
        this.queuedMessagesStrategy = strategy;
    }

    public PersistenceMode getMessageFlowIncomingMode() {
        return this.persistenceModes.get(MESSAGE_FLOW_INCOMING);
    }

    public void setMessageFlowIncomingMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be null");
        LOGGER.debug("Setting incoming message store mode to {}", mode.name());
        this.persistenceModes.put(MESSAGE_FLOW_INCOMING, mode);
    }

    public PersistenceConfig getMessageFlowIncomingConfig() {
        return this.persistenceConfigs.get(MESSAGE_FLOW_INCOMING);
    }

    public void setMessageFlowIncomingConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating incoming msg flow persistence config");
        this.persistenceConfigs.put(MESSAGE_FLOW_INCOMING, config);
    }

    public PersistenceMode getMessageFlowOutgoingMode() {
        return this.persistenceModes.get(MESSAGE_FLOW_OUTGOING);
    }

    public void setMessageFlowOutgoingMode(@NotNull PersistenceMode mode) {
        Preconditions.checkNotNull(mode, "Mode must not be nul");
        LOGGER.debug("Setting outgoing message store mode to {}", mode.name());
        this.persistenceModes.put(MESSAGE_FLOW_OUTGOING, mode);
    }

    public PersistenceConfig getMessageFlowOutgoingConfig() {
        return this.persistenceConfigs.get(MESSAGE_FLOW_OUTGOING);
    }

    public void setMessageFlowOutgoingConfig(@NotNull PersistenceConfig config) {
        Preconditions.checkNotNull(config, "Persistence Config must not be null");
        LOGGER.debug("Updating outgoing msg flow persistence config");
        this.persistenceConfigs.put(MESSAGE_FLOW_OUTGOING, config);
    }
}
