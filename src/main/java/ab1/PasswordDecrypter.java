package ab1;

public class PasswordDecrypter {
    private static byte[] SLAT = {-115, -65, -61, -14, -53, -46, -42, 119, 43, -103, -28, -1, 98, -50, 31, -50, 70, 7, -122, 42, 0, -31, 87, -54, 14, -31, -68, 118, -121, -26, -58, 93, -95, 110, 83, 41, 104, -99, 124, 50, 109, 97, 36, 48, 124, -113, -59, 71, -53, -126};

    public static char[] decrypt(byte[] data) {
        char[] slat = new char[SLAT.length];
        for (int index = 0; index < data.length; index++) {
            slat[index] = ((char) (data[index] ^ SLAT[index]));
        }
        return slat;
    }
}
