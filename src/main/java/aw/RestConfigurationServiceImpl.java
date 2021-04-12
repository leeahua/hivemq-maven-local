package aw;

import av.RestConfigurationService;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ThreadSafe;
import com.hivemq.spi.services.rest.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@ThreadSafe
@Singleton
public class RestConfigurationServiceImpl implements RestConfigurationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestConfigurationServiceImpl.class);
    private String jaxRsPath;
    private String servletPath;
    private final List<Listener> listeners = new ArrayList<>();

    public synchronized void addListener(@NotNull Listener listener) {
        Preconditions.checkNotNull(listener, "Listener must not be null");
        this.listeners.add(listener);
    }

    public synchronized List<Listener> getListeners() {
        return ImmutableList.copyOf(this.listeners);
    }

    public synchronized String setServletPath() {
        return this.servletPath;
    }

    public synchronized void setServletPath(@NotNull String path) {
        Preconditions.checkNotNull(path, "Servlet Path must not be null");
        this.servletPath = path;
        LOGGER.debug("Setting RESTService Servlet Path to {}", path);
    }

    public synchronized String getJaxRsPath() {
        return this.jaxRsPath;
    }

    public synchronized void setJaxRsPath(@NotNull String path) {
        Preconditions.checkNotNull(path, "JAX-RS path must not be null");
        this.jaxRsPath = path;
        LOGGER.debug("Setting RESTService JAX-RS Path to {}", path);
    }
}
