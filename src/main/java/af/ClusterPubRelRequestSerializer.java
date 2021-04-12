package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.hivemq.spi.message.PubRel;
import w1.ClusterPubRelRequest;

public class ClusterPubRelRequestSerializer extends Serializer<ClusterPubRelRequest> {

    @Override
    public void write(Kryo kryo, Output output, ClusterPubRelRequest object) {
        output.writeString(object.getClientId());
        kryo.writeObject(output, object.getPubRel());
    }

    @Override
    public ClusterPubRelRequest read(Kryo kryo, Input input, Class<ClusterPubRelRequest> type) {
        String clientId = input.readString();
        PubRel pubRel = kryo.readObject(input, PubRel.class);
        return new ClusterPubRelRequest(pubRel, clientId);
    }
}
