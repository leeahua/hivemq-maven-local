package bi1;

import cb1.ByteUtils;
import com.google.common.primitives.Longs;

import java.nio.charset.StandardCharsets;
import v.ClientSession;

public class ClientSessionSerializer {

    public byte[] serializeKey(String key) {
        return key.getBytes();
    }

    public String deserializeKey(byte[] keyBytes) {
        return new String(keyBytes, 0, keyBytes.length, StandardCharsets.UTF_8);
    }

    public ClientSession deserialize(byte[] clientSessionBytes) {
        byte temp = clientSessionBytes[8];
        boolean connected = ByteUtils.getBoolean(temp, 0);
        boolean persistentSession = ByteUtils.getBoolean(temp, 1);
        String clusterId = new String(clientSessionBytes, 9, clientSessionBytes.length - 9, StandardCharsets.UTF_8);
        return new ClientSession(connected, persistentSession, clusterId);
    }

    public byte[] serialize(ClientSession clientSession, long timestamp) {
        byte[] data = new byte[clientSession.getConnectedNode().length() + 9];
        System.arraycopy(Longs.toByteArray(timestamp), 0, data, 0, 8);
        data[8] = ByteUtils.getByte((byte)0, 0, clientSession.isConnected());
        data[8] = ByteUtils.getByte(data[8], 1, clientSession.isPersistentSession());
        byte[] clusterIdBytes = clientSession.getConnectedNode().getBytes(StandardCharsets.UTF_8);
        System.arraycopy(clusterIdBytes, 0, data, 9, clusterIdBytes.length);
        return data;
    }

    public long deserializeTimestamp(byte[] data) {
        return ByteUtils.getLong(data, 0);
    }
}
