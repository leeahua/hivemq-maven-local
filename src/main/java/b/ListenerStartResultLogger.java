package b;

import com.google.common.base.Preconditions;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.exceptions.UnrecoverableException;
import com.hivemq.spi.services.configuration.entity.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class ListenerStartResultLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerStartResultLogger.class);
    private final List<ListenerStartResult> startResults;

    public ListenerStartResultLogger(@NotNull List<ListenerStartResult> startResults) {
        Preconditions.checkNotNull(startResults);
        this.startResults = startResults;
    }

    public void log() {
        if (this.startResults.isEmpty()) {
            LOGGER.error("No listener was configured. In order to operate properly, HiveMQ needs at least one listener. Shutting down HiveMQ");
            throw new UnrecoverableException(false);
        }
        int startedCount = 0;
        Iterator<ListenerStartResult> iterator = this.startResults.iterator();
        while (iterator.hasNext()) {
            ListenerStartResult result = iterator.next();
            if (result.isHasException()) {
                LOGGER.info(buildStartSuccessInfo(result));
                startedCount++;
                continue;
            }
            LOGGER.error(buildStartFailedInfo(result));
            if (result.getException().isPresent()) {
                LOGGER.debug("Original Exception:", result.getException().get());
            }
        }
        if (startedCount < 1) {
            LOGGER.error("Could not bind any listener. Stopping HiveMQ.");
            throw new UnrecoverableException(false);
        }
    }

    private String buildStartSuccessInfo(ListenerStartResult startResult) {
        StringBuilder builder = new StringBuilder();
        Listener listener = startResult.getListener();
        builder.append("Started ");
        builder.append(listener.readableName());
        builder.append(" ");
        builder.append("on address ");
        builder.append(listener.getBindAddress());
        builder.append(" and on port ");
        builder.append(startResult.getPort());
        return builder.toString();
    }

    private String buildStartFailedInfo(ListenerStartResult startResult) {
        StringBuilder builder = new StringBuilder();
        Listener listener = startResult.getListener();
        builder.append("Could not start ");
        builder.append(listener.readableName());
        builder.append(" ");
        builder.append("on port ");
        builder.append(startResult.getPort());
        builder.append(" and address ");
        builder.append(listener.getBindAddress());
        builder.append(". Is it already in use?");
        return builder.toString();
    }
}
