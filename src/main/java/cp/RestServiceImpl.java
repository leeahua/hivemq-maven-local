package cp;

import av.RestConfigurationService;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.Injector;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.services.rest.RESTService;
import com.hivemq.spi.services.rest.listener.HttpListener;
import com.hivemq.spi.services.rest.listener.Listener;
import com.hivemq.spi.services.rest.servlet.ServletFilter;
import cq.UnableToStartHttpServerException;
import cr.RestResourceFactory;
import d.CacheScoped;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jboss.resteasy.core.ResourceMethodRegistry;
import org.jboss.resteasy.plugins.guice.GuiceResteasyBootstrapServletContextListener;
import org.jboss.resteasy.plugins.server.servlet.HttpServletDispatcher;
import org.jboss.resteasy.spi.Registry;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpServlet;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@CacheScoped
public class RestServiceImpl implements RESTService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestServiceImpl.class);
    private final Map<String, ServletContextHandler> servletContexts = new HashMap();
    private final Map<String, ServletContextHandler> jaxRsContexts = new HashMap();
    private final Injector injector;
    private final RestConfigurationService restConfigurationService;
    private Server httpServer;
    private HandlerCollection handlerCollection;

    @Inject
    protected RestServiceImpl(Injector injector,
                    RestConfigurationService restConfigurationService) {
        this.injector = injector;
        this.restConfigurationService = restConfigurationService;
    }

    @PostConstruct
    protected void init() {
        LOGGER.debug("Initializing RESTService");
        this.httpServer = new Server();
        this.handlerCollection = new HandlerCollection(true);
        this.httpServer.setHandler(this.handlerCollection);
        List<Listener> listeners = this.restConfigurationService.getListeners();
        if (listeners.size() > 0) {
            LOGGER.debug("Initializing listeners from configuration file");
            listeners.forEach(this::addListener);
        }
    }

    public synchronized void addListener(@NotNull Listener listener) {
        Preconditions.checkNotNull(listener, "Listener must not be null");
        Preconditions.checkArgument(listener instanceof HttpListener, "Listener must be of type HTTPListener");

        HttpListener httpListener = (HttpListener) listener;
        LOGGER.info("Added HTTP Listener with id'{}' on host '{}' and port {}",
                httpListener.getName(), httpListener.getBindAddress(), httpListener.getPort());
        startHttpServer();
        ServerConnector connector = new ServerConnector(this.httpServer, new HttpConnectionFactory());
        connector.setPort(httpListener.getPort());
        connector.setIdleTimeout(30000L);
        connector.setHost(httpListener.getBindAddress());
        connector.setName(httpListener.getName());
        this.httpServer.addConnector(connector);
        try {
            connector.start();
        } catch (Exception e) {
            throw new UnableToStartHttpServerException(e);
        }
        createServletContext(connector);
        createJaxRsContext(connector);
    }

    @ReadOnly
    public synchronized Collection<Listener> getListeners() {
        Connector[] connectors = this.httpServer.getConnectors();
        ImmutableList.Builder builder = ImmutableList.builder();
        Arrays.stream(connectors)
                .map(connector -> (ServerConnector) connector)
                .filter(serverConnector -> HttpVersion.fromString(serverConnector.getProtocols().get(0)) != null)
                .forEach(serverConnector -> {
                    String name = serverConnector.getName();
                    String host = serverConnector.getHost();
                    int port = serverConnector.getPort();
                    builder.add(new HttpListener(name, host, port));
                });
        return builder.build();
    }

    public synchronized void addServlet(@NotNull HttpServlet servlet, @NotNull String path) {
        addServlet(servlet, path, Collections.emptyList());
    }

    public synchronized void addServlet(@NotNull HttpServlet servlet, @NotNull String path, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(servlet, "Servlet must not be null");
        Preconditions.checkNotNull(path, "Path must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listener Identifiers must not be null");
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.forEach(listenerIdentifier -> {
            ServletContextHandler servletContextHandler = this.servletContexts.get(listenerIdentifier);
            if (servletContextHandler != null) {
                LOGGER.info("Added Servlet '{}' with mapping '{}' to listener '{}'",
                        servlet.getClass().getCanonicalName(), path, listenerIdentifier);
                servletContextHandler.addServlet(new ServletHolder(path, servlet), path);
            } else {
                LOGGER.warn("Could not add servlet {} because no listener is configured with identifier '{}'",
                        servlet.getClass(), listenerIdentifier);
            }
        });
    }

    private ServletContextHandler createServletContext(@NotNull String path) {
        ServletContextHandler context = new ServletContextHandler(0);
        context.setContextPath(path);
        context.setServer(this.httpServer);
        return context;
    }

    private ServletContextHandler createJaxRsContext(@NotNull String path) {
        ServletContextHandler context = new ServletContextHandler(0);
        context.setContextPath("/");
        context.setServer(this.httpServer);
        HttpServletDispatcher dispatcher = new HttpServletDispatcher();
        ServletHolder holder = new ServletHolder(dispatcher);
        context.addServlet(holder, path);
        return context;
    }

    public synchronized <T extends HttpServlet> T addServlet(@NotNull Class<T> servlet, @NotNull String path) {
        return addServlet(servlet, path, Collections.emptyList());
    }

    public synchronized <T extends HttpServlet> T addServlet(@NotNull Class<T> servlet, @NotNull String path, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(servlet, "Servlet class must not be null");
        T instance = this.injector.getInstance(servlet);
        addServlet(instance, path, listenerIdentifiers);
        return instance;
    }

    public synchronized <T extends Filter> T addFilter(@NotNull ServletFilter<T> filter, @NotNull String path) {
        return addFilter(filter, path, Collections.emptyList());
    }

    public synchronized <T extends Filter> T addFilter(@NotNull ServletFilter<T> filter, @NotNull String path, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(filter, "Filter must not be null");
        Preconditions.checkNotNull(path, "Path must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        if (filter.getFilterClass().isPresent()) {
            Filter instance = this.injector.getInstance(filter.getFilterClass().get());
            filter = new ServletFilter(instance, filter.getDispatcherTypes().toArray(new DispatcherType[filter.getDispatcherTypes().size()]));
        }
        addFilter(path, listenerIdentifiers, filter);
        return filter.getFilter().get();
    }

    @SafeVarargs
    public final synchronized void addServletWithFilters(@NotNull HttpServlet servlet, @NotNull String path, @NotNull ServletFilter<? extends Filter>... filters) {
        addServletWithFilters(servlet, path, Collections.emptyList(), filters);
    }

    @SafeVarargs
    public final synchronized void addServletWithFilters(@NotNull HttpServlet servlet, @NotNull String path, @NotNull Collection<String> listenerIdentifiers, @NotNull ServletFilter<? extends Filter>... filters) {
        Preconditions.checkNotNull(servlet, "Servlet must not be null");
        Preconditions.checkNotNull(path, "Path must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listener identifiers must not be null");
        Preconditions.checkNotNull(filters, "Servlet filters must not be null");
        addServlet(servlet, path, listenerIdentifiers);
        addFilter(path, listenerIdentifiers, filters);
    }

    @SafeVarargs
    public final synchronized <T extends HttpServlet> T addServletWithFilters(@NotNull Class<T> servlet, @NotNull String path, @NotNull ServletFilter<? extends Filter>... filters) {
        Preconditions.checkNotNull(servlet, "Servlet Class must not be null");
        T instance = this.injector.getInstance(servlet);
        addServletWithFilters(instance, path, filters);
        return instance;
    }

    @SafeVarargs
    public final synchronized <T extends HttpServlet> T addServletWithFilters(@NotNull Class<T> servlet, @NotNull String path, @NotNull Collection<String> listenerIdentifiers, @NotNull ServletFilter<? extends Filter>... filters) {
        Preconditions.checkNotNull(servlet, "Servlet Class must not be null");
        T instance = this.injector.getInstance(servlet);
        addServletWithFilters(instance, path, listenerIdentifiers, filters);
        return instance;
    }

    public synchronized void addJaxRsApplication(@NotNull Application application) {
        addJaxRsApplication(application, Collections.emptyList());
    }

    public synchronized void addJaxRsApplication(@NotNull Application application, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(application, "Application must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        addJaxRsSingletons(application.getSingletons(), listenerIdentifiers);
        addJaxRsResources(application.getClasses(), listenerIdentifiers);
    }

    public synchronized void addJaxRsSingletons(@NotNull Object... singletons) {
        addJaxRsSingletons(Arrays.asList(singletons), Collections.emptyList());
    }

    public synchronized void addJaxRsSingletons(@NotNull Collection<Object> singletons, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(singletons, "Singletons must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.forEach(listenerIdentifier -> {
            ServletContextHandler servletContextHandler = this.jaxRsContexts.get(listenerIdentifier);
            if (servletContextHandler == null) {
                LOGGER.warn("Could not add JAX-RS resources {} because no listener is configured with identifier '{}'", singletons.toString(), listenerIdentifier);
                return;
            }
            singletons.forEach(singleton -> {
                ResourceMethodRegistry methodRegistry = (ResourceMethodRegistry) servletContextHandler.getServletContext().getAttribute(Registry.class.getName());
                methodRegistry.addSingletonResource(singleton);
                LOGGER.info("Adding JAX-RS singleton resource {} to RESTService listener '{}'", singleton.toString(), listenerIdentifier);
            });
        });
    }

    public synchronized void addJaxRsResources(@NotNull Class<?>... resources) {
        addJaxRsResources(Arrays.asList(resources), Collections.emptyList());
    }

    public synchronized void addJaxRsResources(@NotNull Collection<Class<?>> resources, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(resources, "Resources must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.forEach(listenerIdentifier -> {
            ServletContextHandler context = this.jaxRsContexts.get(listenerIdentifier);
            if (context == null) {
                LOGGER.warn("Could not add JAX-RS resources {} because no listener is configured with identifier '{}'",
                        resources.toString(), listenerIdentifier);
                return;
            }
            resources.forEach(resource -> {
                ResourceMethodRegistry methodRegistry = (ResourceMethodRegistry) context.getServletContext().getAttribute(Registry.class.getName());
                methodRegistry.addResourceFactory(new RestResourceFactory(resource, this.injector));
                LOGGER.info("Adding JAX-RS resource {} to RESTService listener '{}'",
                        resource.getCanonicalName(), listenerIdentifier);
            });
        });
    }

    public synchronized void registerExceptionMapper(@NotNull ExceptionMapper<? extends Throwable> exceptionMapper) {
        registerExceptionMapper(exceptionMapper, Collections.emptyList());
    }

    public synchronized void registerExceptionMapper(@NotNull ExceptionMapper<? extends Throwable> exceptionMapper, @NotNull Collection<String> listenerIdentifiers) {
        Preconditions.checkNotNull(exceptionMapper, "Exception Mapper must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.stream()
                .map(this.jaxRsContexts::get)
                .filter(Objects::nonNull)
                .forEach(servletContextHandler -> {
                    ResteasyProviderFactory factory = (ResteasyProviderFactory) servletContextHandler.getServletContext()
                            .getAttribute(ResteasyProviderFactory.class.getName());
                    LOGGER.debug("Added Exception mapper {}", exceptionMapper.getClass().getCanonicalName());
                    factory.register(exceptionMapper);
                });
    }

    public synchronized void registerExceptionMapper(@NotNull Class<? extends ExceptionMapper<? extends Throwable>> exceptionMapper) {
        registerExceptionMapper(exceptionMapper, Collections.emptyList());
    }

    public synchronized void registerExceptionMapper(@NotNull Class<? extends ExceptionMapper<? extends Throwable>> exceptionMapper, @NotNull Collection<String> listenerIdentifiers) {
        registerProvider(exceptionMapper, listenerIdentifiers, "Exception Mapper");
    }

    public synchronized void registerContextResolver(Class<? extends ContextResolver> contextResolver) {
        registerContextResolver(contextResolver, Collections.emptyList());
    }

    public synchronized void registerContextResolver(Class<? extends ContextResolver> contextResolver, Collection<String> listenerIdentifiers) {
        registerProvider(contextResolver, listenerIdentifiers, "Context Resolver");
    }

    public synchronized void registerMessageBodyWriter(Class<? extends MessageBodyWriter> messageBodyWriter) {
        registerMessageBodyWriter(messageBodyWriter, Collections.emptyList());
    }

    public synchronized void registerMessageBodyWriter(Class<? extends MessageBodyWriter> messageBodyWriter, Collection<String> listenerIdentifiers) {
        registerProvider(messageBodyWriter, listenerIdentifiers, "Message Body Writer");
    }

    public synchronized void registerMessageBodyReader(Class<? extends MessageBodyReader> messageBodyReader) {
        registerMessageBodyReader(messageBodyReader, Collections.emptyList());
    }

    public synchronized void registerMessageBodyReader(Class<? extends MessageBodyReader> messageBodyReader, Collection<String> listenerIdentifiers) {
        registerProvider(messageBodyReader, listenerIdentifiers, "Message Body Reader");
    }

    private void registerProvider(Class provider, Collection<String> listenerIdentifiers, String name) {
        Preconditions.checkNotNull(provider, name + " must not be null");
        Preconditions.checkNotNull(listenerIdentifiers, "Listeners must not be null");
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.stream()
                .map(this.jaxRsContexts::get)
                .filter(Objects::nonNull)
                .forEach(servletContextHandler -> {
                    ResteasyProviderFactory providerFactory = (ResteasyProviderFactory) servletContextHandler.getServletContext().getAttribute(ResteasyProviderFactory.class.getName());
                    LOGGER.debug("Added {} {}", name, provider.getCanonicalName());
                    providerFactory.register(provider);
                });
    }

    @SafeVarargs
    private final void addFilter(@NotNull String path, @NotNull Collection<String> listenerIdentifiers, @NotNull ServletFilter<? extends Filter>... filters) {
        if (listenerIdentifiers.isEmpty()) {
            listenerIdentifiers = getAllListenerIdentifiers();
        }
        listenerIdentifiers.stream()
                .map(this.servletContexts::get)
                .filter(Objects::nonNull)
                .forEach(listenerIdentifier -> {
                    for (ServletFilter<? extends Filter> servletFilter : filters) {
                        Filter filter = safeFilter(servletFilter);
                        LOGGER.info("Adding filter {} to listener '{}' with path '{}'", filter.getClass(), listenerIdentifier, path);
                        ServletContextHandler context = this.servletContexts.get(listenerIdentifier);
                        context.addFilter(new FilterHolder(filter), path, EnumSet.copyOf(servletFilter.getDispatcherTypes()));
                    }
                });
    }

    private Collection<String> getAllListenerIdentifiers() {
        ImmutableList.Builder builder = ImmutableList.builder();
        Connector[] connectors = this.httpServer.getConnectors();
        Arrays.stream(connectors)
                .map(Connector::getName)
                .forEach(builder::add);
        return builder.build();
    }

    private void createServletContext(Connector connector) {
        LOGGER.trace("Creating servlet context for connector {}", connector.getName());
        ServletContextHandler context = this.servletContexts.get(connector.getName());
        if (context != null) {
            return;
        }
        context = createServletContext(this.restConfigurationService.setServletPath());
        context.setVirtualHosts(new String[]{"@" + connector.getName()});
        try {
            context.start();
        } catch (Exception e) {
            throw new UnableToStartHttpServerException(e);
        }
        this.servletContexts.put(connector.getName(), context);
        this.handlerCollection.addHandler(context);
    }

    private void createJaxRsContext(Connector connector) {
        LOGGER.trace("Creating JAX-RS servlet context for connector {}", connector.getName());
        ServletContextHandler context = this.jaxRsContexts.get(connector.getName());
        if (context != null) {
            return;
        }
        context = createJaxRsContext(this.restConfigurationService.getJaxRsPath());
        context.setVirtualHosts(new String[]{"@" + connector.getName()});
        context.addEventListener(this.injector.getInstance(GuiceResteasyBootstrapServletContextListener.class));
        try {
            context.start();
        } catch (Exception e) {
            throw new UnableToStartHttpServerException(e);
        }
        this.jaxRsContexts.put(connector.getName(), context);
        this.handlerCollection.addHandler(context);
    }

    private void startHttpServer() {
        if (this.httpServer.isStarted()) {
            return;
        }
        LOGGER.info("Starting HTTP Server...");
        long startMillis = System.currentTimeMillis();
        try {
            this.httpServer.start();
        } catch (Exception e) {
            LOGGER.error("Unable to start HTTP Server!", e);
            throw new UnableToStartHttpServerException(e);
        }
        LOGGER.info("Started HTTP Server in {}ms", System.currentTimeMillis() - startMillis);
    }

    private <T extends Filter> T safeFilter(ServletFilter<T> filter) {
        if (filter.getFilterClass().isPresent()) {
            return this.injector.getInstance(filter.getFilterClass().get());
        }
        return filter.getFilter().get();
    }

    @VisibleForTesting
    protected Server getHttpServer() {
        return this.httpServer;
    }
}
