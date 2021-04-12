package bg1;

import bu.InternalPublish;
import com.hivemq.spi.message.MessageWithId;
import com.hivemq.spi.message.Publish;

import java.util.Comparator;

public class MessageWithIdComparator implements Comparator<MessageWithId> {
    @Override
    public int compare(MessageWithId o1, MessageWithId o2) {
        if (o1 instanceof InternalPublish && o2 instanceof InternalPublish) {
            InternalPublish publish1 = (InternalPublish) o1;
            InternalPublish publish2 = (InternalPublish) o2;
            int result = Long.compare(publish1.getTimestamp(), publish2.getTimestamp());
            if (result != 0) {
                return result;
            }
            if (publish1.getClusterId().equals(publish2.getClusterId())) {
                return Long.compare(publish1.getSequence(), publish2.getSequence());
            }
            return publish1.getClusterId().compareTo(publish2.getClusterId());
        }
        if (o1 instanceof Publish && o2 instanceof Publish) {
            throw new IllegalArgumentException("Comparator only supports InternalPUBLISH");
        }
        if (o1 instanceof InternalPublish) {
            return 1;
        }
        if (o2 instanceof InternalPublish) {
            return -1;
        }
        return Integer.compare(o1.getMessageId(), o2.getMessageId());
    }
}
