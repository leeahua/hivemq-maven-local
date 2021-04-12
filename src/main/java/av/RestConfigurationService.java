package av;

import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.services.rest.listener.Listener;

import java.util.List;

public interface RestConfigurationService {
    void addListener(@NotNull Listener listener);

    @ReadOnly
    List<Listener> getListeners();

    String setServletPath();

    void setServletPath(@NotNull String path);

    String getJaxRsPath();

    void setJaxRsPath(@NotNull String path);
}
