package aj1;

import com.google.common.base.Preconditions;

public class MacAddressFormatter {
    public static final int HARDWARE_ADDRESS_LENGTH = 6;

    public static String format(byte[] hardwareAddress) {
        Preconditions.checkNotNull(hardwareAddress);
        Preconditions.checkArgument(hardwareAddress.length == HARDWARE_ADDRESS_LENGTH,
                "Hardware address must be of length %s but was %s",
                HARDWARE_ADDRESS_LENGTH, hardwareAddress.length);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < hardwareAddress.length; index++) {
            builder.append(String.format("%02X%s",
                    Byte.valueOf(hardwareAddress[index]),
                    index < hardwareAddress.length - 1 ? "-" : ""));
        }
        return builder.toString();
    }
}
