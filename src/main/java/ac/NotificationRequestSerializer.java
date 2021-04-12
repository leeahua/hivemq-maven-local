package ac;

import ah.ClusterState;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.ClusterStateNotificationRequest;

public class NotificationRequestSerializer extends Serializer<ClusterStateNotificationRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClusterStateNotificationRequest object) {
        output.writeString(object.getNode());
        output.writeInt(object.getState().getValue());
    }

    @Override
    public ClusterStateNotificationRequest read(Kryo kryo, Input input, Class<ClusterStateNotificationRequest> type) {
        String node = input.readString();
        int stateValue = input.readInt();
        ClusterState state = ClusterState.valueOf(stateValue);
        return new ClusterStateNotificationRequest(node, state);
    }
}
