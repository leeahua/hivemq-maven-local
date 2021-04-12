package af1;

import aa1.LicenseType;
import ae1.LicenseInformation;
import ae1.LicenseInformationReader;
import ai1.LicenseCheckScheduler;
import ai1.LicenseInformationFilter;
import ai1.LicenseInformationProducer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class LicenseInformationService implements Provider<LicenseInformation> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseInformationService.class);
    private final LicenseInformationFilter licenseInformationFilter;
    private final LicenseInformationProducer licenseInformationProducer;
    private final Provider<LicenseCheckScheduler> licenseCheckSchedulerProvider;
    private final LicenseInformationReader licenseInformationReader;
    private final SystemInformation systemInformation;
    private LicenseInformation currentLicense = null;
    private boolean dailyLicenseCheckIsRunning = false;
    private final List<OnChangeListener> listeners = new CopyOnWriteArrayList<>();

    @Inject
    public LicenseInformationService(LicenseInformationFilter licenseInformationFilter,
                                     LicenseInformationProducer licenseInformationProducer,
                                     Provider<LicenseCheckScheduler> licenseCheckSchedulerProvider,
                                     LicenseInformationReader licenseInformationReader,
                                     SystemInformation systemInformation) {
        this.licenseInformationFilter = licenseInformationFilter;
        this.licenseInformationProducer = licenseInformationProducer;
        this.licenseCheckSchedulerProvider = licenseCheckSchedulerProvider;
        this.licenseInformationReader = licenseInformationReader;
        this.systemInformation = systemInformation;
    }

    @Override
    public synchronized LicenseInformation get() {
        if (this.currentLicense == null) {
            dailyCheck();
        }
        return this.currentLicense;
    }

    public void dailyCheck() {
        List<LicenseInformation> licenseInformations = this.licenseInformationProducer.produce(getLicenseFiles());
        LicenseType licenseType = this.currentLicense == null ? LicenseType.NONE : this.currentLicense.getLicenseType();
        LicenseInformation optimalLicense = this.licenseInformationFilter.optimal(licenseInformations, licenseType);
        if (licenseType != LicenseType.NONE &&
                optimalLicense.getLicenseType() != LicenseType.NONE &&
                this.currentLicense.getLicenseType() != optimalLicense.getLicenseType()) {
            return;
        }
        if (this.currentLicense == null ||
                !this.currentLicense.getId().equals(optimalLicense.getId())) {
            onChange(optimalLicense);
            this.currentLicense = optimalLicense;
        }
    }

    public void register(OnChangeListener listener) {
        this.listeners.add(listener);
    }

    private void onChange(LicenseInformation optimalLicense) {
        print(optimalLicense);
        if (!this.dailyLicenseCheckIsRunning &&
                optimalLicense.getLicenseType() == LicenseType.CONNECTION_LIMITED) {
            this.licenseCheckSchedulerProvider.get().schedule(() -> {
                LOGGER.trace("Running daily license check");
                dailyCheck();
            });
            this.dailyLicenseCheckIsRunning = true;
        }
        if (this.currentLicense != null &&
                !this.currentLicense.getId().equals(optimalLicense.getId())) {
            this.listeners.forEach(listener ->
                    listener.onChange(this.currentLicense, optimalLicense));
        }
    }


    @VisibleForTesting
    protected List<File> getLicenseFiles() {
        File licenseFolder = this.systemInformation.getLicenseFolder();
        if (!licenseFolder.exists() || !licenseFolder.isDirectory()) {
            LOGGER.debug("License folder '{}' not found", licenseFolder.getAbsolutePath());
            return Lists.newArrayList();
        }
        File[] licenseFiles = licenseFolder.listFiles((dir, name) -> name.endsWith(".lic"));
        return Arrays.asList(licenseFiles);
    }

    private void print(LicenseInformation licenseInformation) {
        LOGGER.info(this.licenseInformationReader.read(licenseInformation));
    }

    public interface OnChangeListener {
        void onChange(LicenseInformation oldLicense, LicenseInformation newLicense);
    }
}
