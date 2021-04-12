package cr;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.hivemq.spi.annotations.NotNull;
import org.jboss.resteasy.plugins.guice.ext.JaxrsModule;
import org.jboss.resteasy.plugins.guice.ext.RequestScopeModule;
import org.jboss.resteasy.plugins.guice.ext.ResponseBuilderProvider;
import org.jboss.resteasy.plugins.guice.ext.UriBuilderProvider;
import org.jboss.resteasy.plugins.guice.ext.VariantListBuilderProvider;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.HttpResponse;
import org.jboss.resteasy.spi.PropertyInjector;
import org.jboss.resteasy.spi.ResourceFactory;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

public class RestResourceFactory implements ResourceFactory {
    private final Class<?> resource;
    private final Injector parentInject;
    private PropertyInjector propertyInjector;
    private Injector restInjector;

    public RestResourceFactory(@NotNull Class<?> resource, @NotNull Injector parentInject) {
        Preconditions.checkNotNull(resource, "Resource must not be null");
        Preconditions.checkNotNull(parentInject, "The parent injector must not be null");
        this.resource = resource;
        this.parentInject = parentInject;
    }

    public Class<?> getScannableClass() {
        return this.resource;
    }

    public void registered(ResteasyProviderFactory resteasyProviderFactory) {
        this.restInjector = this.parentInject.createChildInjector(new RestModule(), new RequestScopeModule());
        this.propertyInjector = resteasyProviderFactory.getInjectorFactory()
                .createPropertyInjector(this.resource, resteasyProviderFactory);
    }

    public Object createResource(HttpRequest request, HttpResponse response, ResteasyProviderFactory factory) {
        Object instance = this.restInjector.getInstance(this.resource);
        this.propertyInjector.inject(request, response, instance);
        return instance;
    }

    public void requestFinished(HttpRequest request, HttpResponse response, Object resource) {
    }

    public void unregistered() {
    }

    public class RestModule extends JaxrsModule {
        public RestModule() {
        }

        public void configure(Binder binder) {
            binder.bind(RuntimeDelegate.class).toInstance(RuntimeDelegate.getInstance());
            binder.bind(Response.ResponseBuilder.class).toProvider(ResponseBuilderProvider.class);
            binder.bind(UriBuilder.class).toProvider(UriBuilderProvider.class);
            binder.bind(Variant.VariantListBuilder.class).toProvider(VariantListBuilderProvider.class);
        }
    }
}
