package af;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import y1.LicenseInformationResult;
import y1.LicenseState;

public class LicenseInformationResultSerializer
        extends Serializer<LicenseInformationResult> {

    @Override
    public void write(Kryo kryo, Output output, LicenseInformationResult object) {
        output.writeInt(object.getLicenseState().ordinal());
        kryo.writeObjectOrNull(output, object.getLicenseId(), String.class);
    }

    @Override
    public LicenseInformationResult read(Kryo kryo, Input input, Class<LicenseInformationResult> type) {
        LicenseState licenseState = LicenseState.values()[input.readInt()];
        String licenseId = kryo.readObjectOrNull(input, String.class);
        return new LicenseInformationResult(licenseState, licenseId);
    }
}
