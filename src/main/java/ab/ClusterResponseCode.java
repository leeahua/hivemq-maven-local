package ab;

import com.hivemq.spi.annotations.Nullable;
import p.ClusterResponseDeniedException;
import p.ClusterBusyException;
import p.ClusterNotResponsibleException;
import p.ClusterRequestFailedException;

public enum ClusterResponseCode {
    OK(0),                  //a(1),
    DENIED(1),              //b(2),
    NOT_RESPONSIBLE(2),     //c(3),
    BUSY(3),                //d(4),
    FAILED(4);              //e(5);

    private final int value;

    ClusterResponseCode(int value) {
        this.value = value;
    }

    public int getValue() {
        return this.value;
    }

    public static ClusterResponseCode valueOf(int value) {
        for (ClusterResponseCode code : values()) {
            if (code.getValue() == value) {
                return code;
            }
        }
        throw new IllegalArgumentException("No state found for the given value : " + value + ".");
    }

    @Nullable
    public Exception createException() {
        switch (this) {
            case OK:
                throw new IllegalArgumentException("There is no exception for the response code 'OK'");
            case DENIED:
                return new ClusterResponseDeniedException();
            case NOT_RESPONSIBLE:
                return new ClusterNotResponsibleException();
            case BUSY:
                return new ClusterBusyException();
            case FAILED:
                return new ClusterRequestFailedException();
        }
        return null;
    }
}
