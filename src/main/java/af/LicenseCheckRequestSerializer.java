package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.LicenseState;
import y1.LicenseCheckRequest;

public class LicenseCheckRequestSerializer
        extends Serializer<LicenseCheckRequest> {

    @Override
    public void write(Kryo kryo, Output output, LicenseCheckRequest object) {
        output.writeInt(object.getLicenseState().ordinal());
        kryo.writeObjectOrNull(output, object.getLicenseId(), String.class);
    }

    @Override
    public LicenseCheckRequest read(Kryo kryo, Input input, Class<LicenseCheckRequest> type) {
        LicenseState licenseState = LicenseState.values()[input.readInt()];
        String licenseId = kryo.readObjectOrNull(input, String.class);
        return new LicenseCheckRequest(licenseState, licenseId);
    }
}
