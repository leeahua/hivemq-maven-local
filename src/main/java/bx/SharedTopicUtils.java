package bx;

import cb1.ByteUtils;

public class SharedTopicUtils {
    public static final int SHARED_TOPIC_BYTE = 0;
    public static final int SHARED_TOPIC_BYTE_POSITION = 1;

    public static byte getShared(boolean sharedTopic) {
        return ByteUtils.getByte((byte) SHARED_TOPIC_BYTE, SHARED_TOPIC_BYTE_POSITION, sharedTopic);
    }
}
