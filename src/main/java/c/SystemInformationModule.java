package c;

import com.google.inject.AbstractModule;
import com.hivemq.spi.config.SystemInformation;

public class SystemInformationModule extends AbstractModule {
    private final SystemInformation systemInformation;

    public SystemInformationModule(SystemInformation systemInformation) {
        this.systemInformation = systemInformation;
    }

    protected void configure() {
        bind(SystemInformation.class).toInstance(this.systemInformation);
    }
}
