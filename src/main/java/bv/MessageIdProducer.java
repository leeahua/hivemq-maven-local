package bv;

import bw.ProduceMessageIdException;
import com.hivemq.spi.annotations.ThreadSafe;

@ThreadSafe
public interface MessageIdProducer {
    @ThreadSafe
    int next() throws ProduceMessageIdException;

    @ThreadSafe
    int next(int messageId) throws ProduceMessageIdException;

    @ThreadSafe
    void remove(int messageId);

    @ThreadSafe
    void add(int... messageIds);
}
