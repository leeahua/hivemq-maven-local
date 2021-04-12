package bi;

import bu.CachedPubAck;
import bu.CachedPubComp;
import bu.CachedPubRec;
import bu.CachedPubRel;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.hivemq.spi.message.PubAck;
import com.hivemq.spi.message.PubComp;
import com.hivemq.spi.message.PubRec;
import com.hivemq.spi.message.PubRel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CachedMessages {
    private static final Logger LOGGER = LoggerFactory.getLogger(CachedMessages.class);
    public static CachedMessages INSTANCE = new CachedMessages();
    private final int MAX_MESSAGE_ID = 65535;
    @VisibleForTesting
    protected final ConcurrentMap<Integer, PubAck> pubAcks = new ConcurrentHashMap();
    @VisibleForTesting
    protected final ConcurrentMap<Integer, PubRec> pubRecs = new ConcurrentHashMap();
    @VisibleForTesting
    protected final ConcurrentMap<Integer, PubRel> pubRels = new ConcurrentHashMap();
    @VisibleForTesting
    protected final ConcurrentMap<Integer, PubComp> pubComps = new ConcurrentHashMap();

    private CachedMessages() {
        this.LOGGER.debug("Message cache is enabled.");
        for (int messageId = 0; messageId <= MAX_MESSAGE_ID; messageId++) {
            this.pubAcks.put(messageId, new CachedPubAck(messageId));
            this.pubRecs.put(messageId, new CachedPubRec(messageId));
            this.pubRels.put(messageId, new CachedPubRel(messageId));
            this.pubComps.put(messageId, new CachedPubComp(messageId));
        }
        this.LOGGER.trace("Initialized PubAck object cache.");
        this.LOGGER.trace("Initialized PubRec object cache.");
        this.LOGGER.trace("Initialized PubRel object cache.");
        this.LOGGER.trace("Initialized PubComp object cache.");
    }

    public PubAck getPubAck(int messageId) {
        Preconditions.checkArgument(messageId <= MAX_MESSAGE_ID, "Message %s id is invalid. Max message id is 65535.", messageId);
        Preconditions.checkArgument(messageId >= 0, "Message %s id is invalid. Min message id is 0.", messageId);
        return this.pubAcks.get(messageId);
    }

    public PubRec getPubRec(int messageId) {
        Preconditions.checkArgument(messageId <= MAX_MESSAGE_ID, "Message %s id is invalid. Max message id is 65535.", messageId);
        Preconditions.checkArgument(messageId >= 0, "Message %s id is invalid. Min message id is 0.", messageId);
        return this.pubRecs.get(messageId);
    }

    public PubRel getPubRel(int messageId) {
        Preconditions.checkArgument(messageId <= MAX_MESSAGE_ID, "Message %s id is invalid. Max message id is 65535.", messageId);
        Preconditions.checkArgument(messageId >= 0, "Message %s id is invalid. Min message id is 0.", messageId);
        return this.pubRels.get(messageId);
    }

    public PubComp getPubComp(int messageId) {
        Preconditions.checkArgument(messageId <= MAX_MESSAGE_ID, "Message %s id is invalid. Max message id is 65535.", messageId);
        Preconditions.checkArgument(messageId >= 0, "Message %s id is invalid. Min message id is 0.", messageId);
        return this.pubComps.get(messageId);
    }
}
