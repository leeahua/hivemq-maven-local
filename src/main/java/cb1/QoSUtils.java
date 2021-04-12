package cb1;

import com.hivemq.spi.message.QoS;

public class QoSUtils {

    public static QoS min(QoS qoS1, QoS qoS2) {
        return qoS1.getQosNumber() < qoS2.getQosNumber() ? qoS1 : qoS2;
    }
}
