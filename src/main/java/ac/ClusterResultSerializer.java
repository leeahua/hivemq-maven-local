package ac;

import ab.ClusterResponseCode;
import ab.ClusterResult;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

public class ClusterResultSerializer extends Serializer<ClusterResult> {

    @Override
    public void write(Kryo kryo, Output output, ClusterResult object) {
        output.writeInt(object.getCode().getValue());
        output.writeBoolean(object.isValidLicense());
        kryo.writeClassAndObject(output, object.getData());
    }

    @Override
    public ClusterResult read(Kryo kryo, Input input, Class<ClusterResult> type) {
        int codeValue = input.readInt();
        boolean validLicense = input.readBoolean();
        ClusterResponseCode code = ClusterResponseCode.valueOf(codeValue);
        Object data = kryo.readClassAndObject(input);
        return new ClusterResult(code, data, validLicense);
    }
}
