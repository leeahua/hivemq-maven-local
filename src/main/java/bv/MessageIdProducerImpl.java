package bv;

import bw.ProduceMessageIdException;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Ints;
import com.hivemq.spi.annotations.ThreadSafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@ThreadSafe
public class MessageIdProducerImpl implements MessageIdProducer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageIdProducerImpl.class);
    private static final ProduceMessageIdException EXCEPTION = new ProduceMessageIdException();
    private final AtomicInteger maxMessageId = new AtomicInteger();
    private final Set<Integer> messageIds = new HashSet<>();

    static {
        EXCEPTION.setStackTrace(new StackTraceElement[0]);
    }

    @ThreadSafe
    public synchronized int next() throws ProduceMessageIdException {
        return getNext();
    }

    @ThreadSafe
    public synchronized int next(int messageId) throws ProduceMessageIdException {
        Preconditions.checkArgument(messageId > 0);
        Preconditions.checkArgument(messageId <= 65535);
        if (this.messageIds.contains(messageId)) {
            return getNext();
        }
        this.messageIds.add(messageId);
        if (messageId > this.maxMessageId.get()) {
            this.maxMessageId.set(messageId);
        }
        return messageId;
    }

    private int getNext() throws ProduceMessageIdException {
        if (this.messageIds.size() >= 65535) {
            throw EXCEPTION;
        }
        this.maxMessageId.compareAndSet(65535, 0);
        int messageId;
        do {
            messageId = this.maxMessageId.incrementAndGet();
        } while (this.messageIds.contains(messageId) && messageId <= 65536);
        if (messageId > 65535) {
            this.maxMessageId.compareAndSet(65536, 0);
            throw EXCEPTION;
        }
        this.messageIds.add(messageId);
        return messageId;
    }

    @ThreadSafe
    public synchronized void remove(int messageId) {
        Preconditions.checkArgument(messageId > 0);
        Preconditions.checkArgument(messageId <= 65535);
        boolean removed = this.messageIds.remove(messageId);
        if (!removed) {
            LOGGER.trace("Tried to return message id {} although it was already returned. This is could mean a DUP was acked",
                    messageId);
        }
    }

    @ThreadSafe
    public synchronized void add(int... messageIds) {
        for (int index = 0; index < messageIds.length; index++) {
            int messageId = messageIds[index];
            Preconditions.checkArgument(messageId > 0);
            Preconditions.checkArgument(messageId <= 65535);
        }
        List<Integer> messageIdList = Ints.asList(messageIds);
        Collections.sort(messageIdList);
        this.maxMessageId.set(messageIdList.get(messageIdList.size() - 1));
        this.messageIds.addAll(messageIdList);
    }
}
