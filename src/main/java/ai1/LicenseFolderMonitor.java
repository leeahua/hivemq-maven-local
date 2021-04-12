package ai1;

import af1.LicenseInformationService;
import cb1.DirectoryMonitor;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.config.SystemInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.io.File;

public class LicenseFolderMonitor {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseFolderMonitor.class);
    private final LicenseInformationService licenseInformationService;
    private final DirectoryMonitor directoryMonitor;
    private final DirectoryListener listener;

    @Inject
    public LicenseFolderMonitor(LicenseInformationService licenseInformationService,
                                SystemInformation systemInformation) {
        this.licenseInformationService = licenseInformationService;
        File licenseFolder = systemInformation.getLicenseFolder();
        this.listener = new DirectoryListener();
        this.directoryMonitor = new DirectoryMonitor(licenseFolder.getAbsolutePath(), this.listener);
    }

    @PostConstruct
    public void start() {
        this.directoryMonitor.start();
    }

    public void stop() {
        this.directoryMonitor.stop();
    }

    private void onChanged(String path) {
        if (path == null) {
            return;
        }
        if (!path.equals(this.licenseInformationService.get().getFileName())) {
            return;
        }
        this.licenseInformationService.dailyCheck();
    }

    private void onDeleted(String path) {
        if (path == null) {
            return;
        }
        this.licenseInformationService.dailyCheck();
    }

    @VisibleForTesting
    protected DirectoryListener getListener() {
        return this.listener;
    }

    public boolean isWatching() {
        return this.directoryMonitor.isWatching();
    }

    class DirectoryListener implements DirectoryMonitor.Listener {
        DirectoryListener() {
        }

        public void onModify(String watchPath, String target) {
            LicenseFolderMonitor.this.onChanged(target);
        }

        public void onDeleted(String watchPath, String target) {
            LicenseFolderMonitor.this.onDeleted(target);
        }

        public void onCreated(String watchPath, String target) {
            LicenseFolderMonitor.this.onChanged(target);
        }

        public void onError(String watchPath, Throwable cause) {
            LOGGER.debug("License folder is not readable, please check your folder permissions. Runtime license update temporarily disabled. ");
            directoryMonitor.stop();
        }
    }
}
