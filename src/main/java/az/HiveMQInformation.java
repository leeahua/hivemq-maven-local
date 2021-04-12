package az;

import com.hivemq.spi.config.SystemInformation;

import javax.inject.Inject;

public class HiveMQInformation extends AbstractInformation {
    private final SystemInformation systemInformation;

    @Inject
    public HiveMQInformation(SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    public String get() {
        return String.format("[%s] = [%s]\n",
                "HiveMQ Version",
                this.systemInformation.getHiveMQVersion());
    }
}
