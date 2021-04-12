package cw;

import an.HiveMQIdProvider;
import com.hivemq.spi.services.ConfigurationService;
import com.hivemq.update.entity.UpdateRequest;
import com.hivemq.update.entity.UpdateResponse;
import cx.UpdateAttributeKeys;
import cx.UpdateHttpClient;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class UpdateNotificationJob implements UpdateAttributeKeys, Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNotificationJob.class);
    private final UpdateRequestFactory requestFactory;
    private final UpdateHttpClient httpClient;
    private final HiveMQIdProvider hiveMQIdProvider;
    private final ConfigurationService configurationService;

    @Inject
    UpdateNotificationJob(UpdateRequestFactory requestFactory,
                          UpdateHttpClient httpClient,
                          HiveMQIdProvider hiveMQIdProvider,
                          ConfigurationService configurationService) {
        this.requestFactory = requestFactory;
        this.httpClient = httpClient;
        this.hiveMQIdProvider = hiveMQIdProvider;
        this.configurationService = configurationService;
    }

    public void run() {
        boolean enabled = this.configurationService.generalConfiguration().updateCheckEnabled();
        if (!enabled) {
            return;
        }
        String id = this.hiveMQIdProvider.get();
        UpdateRequest request = this.requestFactory.create(id);
        this.httpClient.checkForUpdate(request).addListener(new ChannelFutureListener(){

            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    LOGGER.warn("Could not check for updates");
                    LOGGER.debug("The update check probably timed out");
                    return;
                }
                UpdateResponse response = future.channel().attr(UpdateAttributeKeys.RESPONSE).getAndRemove();
                if (response == null) {
                    LOGGER.warn("Could not check for updates");
                    LOGGER.debug("Did not receive valid response from update server");
                    return;
                }
                UpdateResponse.HiveMQResponse hiveMQResponse = response.getHiveMQResponse();
                if (hiveMQResponse != null) {
                    LOGGER.info("A new HiveMQ Version ({}) is available. Visit {} for more details.",
                            hiveMQResponse.getVersion(), hiveMQResponse.getUrl());
                } else {
                    LOGGER.debug("HiveMQ is up to date.");
                }
            }
        });
    }
}
