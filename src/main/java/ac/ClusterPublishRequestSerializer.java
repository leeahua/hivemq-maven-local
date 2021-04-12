package ac;

import bu.InternalPublish;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import w1.ClusterPublishRequest;
// TODO:
public class ClusterPublishRequestSerializer extends Serializer<ClusterPublishRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClusterPublishRequest object) {
        output.writeInt(object.getQoSNumber());
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getPublish());
        output.writeBoolean(object.d());
        output.writeBoolean(object.e());
        output.writeBoolean(object.f());
    }

    @Override
    public ClusterPublishRequest read(Kryo kryo, Input input, Class<ClusterPublishRequest> type) {
        int qoSNumber = input.readInt();
        String clientId = input.readString();
        InternalPublish publish = kryo.readObject(input, InternalPublish.class);
        boolean bool1 = input.readBoolean();
        boolean bool2 = input.readBoolean();
        boolean bool3 = input.readBoolean();
        return new ClusterPublishRequest(publish, qoSNumber, clientId, bool1, bool2, bool3);
    }
}
