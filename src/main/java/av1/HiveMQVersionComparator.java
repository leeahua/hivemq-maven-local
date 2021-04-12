package av1;

import java.util.Comparator;

public class HiveMQVersionComparator implements Comparator<String> {
    @Override
    public int compare(String o1, String o2) {
        String[] splitVersion1 = o1.split("\\.");
        String[] splitVersion2 = o2.split("\\.");
        if (splitVersion1.length != 3) {
            throw new IllegalArgumentException("Not able to parse HiveMQ version number [" + o1 + "]");
        }
        if (splitVersion2.length != 3) {
            throw new IllegalArgumentException("Not able to parse HiveMQ version number [" + o2 + "]");
        }
        int result = Integer.compare(Integer.parseInt(splitVersion1[0]), Integer.parseInt(splitVersion2[0]));
        if (result != 0) {
            return result;
        }
        result = Integer.compare(Integer.parseInt(splitVersion1[1]), Integer.parseInt(splitVersion2[1]));
        if (result != 0) {
            return result;
        }
        return Integer.compare(Integer.parseInt(splitVersion1[2]), Integer.parseInt(splitVersion2[2]));
    }
}
