package ah1;

import aa1.LicenseType;
import ae1.LicenseInformation;
import ae1.License;
import af1.LicenseInformationService;
import aq1.MqttConnectionCounter;
import com.google.common.annotations.VisibleForTesting;
import com.hivemq.spi.message.Connect;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@ChannelHandler.Sharable
public class LicenseConnectionLimiter extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseConnectionLimiter.class);
    private final LicenseInformationService licenseInformationService;
    private final MqttConnectionCounter connectionCounter;
    private volatile long maximumConnections;
    private volatile long connectionsWarnThreshold;
    private boolean limit = true;

    @Inject
    LicenseConnectionLimiter(LicenseInformationService licenseInformationService,
                             MqttConnectionCounter connectionCounter) {
        this.licenseInformationService = licenseInformationService;
        this.connectionCounter = connectionCounter;
    }

    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        LicenseInformation licenseInformation = this.licenseInformationService.get();
        if (licenseInformation.getLicenseType() == LicenseType.NONE) {
            useDefault();
        } else {
            useSiteLicense(licenseInformation.getLicense());
        }
        if (!this.limit) {
            ctx.pipeline().remove(this);
        }
        super.channelActive(ctx);
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Connect) {
            LicenseInformation licenseInformation = this.licenseInformationService.get();
            long currentConnections = this.connectionCounter.currentConnections();
            if (currentConnections > this.maximumConnections) {
                LOGGER.warn("Maximum connections ({}) for your license already reached. Closing client connection. Please contact {} for a license upgrade or visit {}",
                        this.maximumConnections, licenseInformation.getVendorEmail(), licenseInformation.getVendorWebsite());
                ctx.close();
                return;
            }
            if (this.connectionsWarnThreshold > 0L &&
                    currentConnections >= this.connectionsWarnThreshold) {
                LOGGER.warn("You will reach your maximum connection limit soon. ({} of {} connections already used). Please contact {} for a license upgrade or visit {}",
                        currentConnections, this.maximumConnections, licenseInformation.getVendorEmail(), licenseInformation.getVendorWebsite());
            }
            ctx.pipeline().remove(this);
        }
        super.channelRead(ctx, msg);
    }

    private void useSiteLicense(License license) {
        this.maximumConnections = license.getMaximumConnections();
        this.connectionsWarnThreshold = license.getConnectionsWarnThreshold();
        if (maximumConnections == 0L) {
            this.limit = false;
        }
    }

    private void useDefault() {
        this.maximumConnections = LicenseInformation.DEFAULT_MAXIMUM_CONNECTIONS;
        this.connectionsWarnThreshold = LicenseInformation.DEFAULT_CONNECTIONS_WARN_THRESHOLD;
    }

    @VisibleForTesting
    public long getMaximumConnections() {
        return maximumConnections;
    }

    @VisibleForTesting
    public long getConnectionsWarnThreshold() {
        return connectionsWarnThreshold;
    }

    @VisibleForTesting
    public boolean isLimit() {
        return limit;
    }
}
