package co;

import com.hivemq.spi.message.Publish;

import java.util.concurrent.ExecutorService;

public interface InternalPublishService {
    void publish(Publish publish, ExecutorService executorService);

    void publishToClient(Publish publish, String clientId, ExecutorService executorService);
}
