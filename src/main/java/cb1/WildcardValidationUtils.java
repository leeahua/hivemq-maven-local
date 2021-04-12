package cb1;

import com.hivemq.spi.annotations.NotNull;

// TODO:
public class WildcardValidationUtils {

    public static boolean validPublishTopic(@NotNull String topic) {
        if (topic.isEmpty()) {
            return false;
        }
        if (topic.contains("\000")) {
            return false;
        }
        return topic.indexOf("#") <= -1 && topic.indexOf("+") <= -1;
    }

    public static boolean validSubscribeTopic(@NotNull String topic) {
        if (topic.isEmpty()) {
            return false;
        }
        if (topic.contains("\000")) {
            return false;
        }
        int i = topic.charAt(0);
        int k = topic.length();
        for (int m = 1; m < k; m++) {
            int j = topic.charAt(m);
            if ((m == k - 1) && (j == 35) && (i == 47)) {
                return true;
            }
            if ((i == 35) || ((j == 35) && (m == k - 1))) {
                return false;
            }
            if ((j == 43) && (i != 47)) {
                return false;
            }
            if ((i == 43) && (j != 47)) {
                return false;
            }
            i = j;
        }
        return true;
    }
}
