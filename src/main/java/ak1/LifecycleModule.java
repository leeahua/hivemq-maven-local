package ak1;

import c.BaseModule;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LifecycleModule extends BaseModule<LifecycleModule> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleModule.class);

    public LifecycleModule() {
        super(LifecycleModule.class);
    }

    protected void configure() {
        PreDestroyRegistry preDestroyRegistry = new PreDestroyRegistry();
        bind(PreDestroyRegistry.class).toInstance(preDestroyRegistry);
        bind(LifecycleShutdownRegister.class).asEagerSingleton();
        bindListener(Matchers.any(), new TypeListener() {
            @Override
            public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
                LifecycleModule.this.hear(encounter, type.getRawType(), preDestroyRegistry);
            }
        });
    }

    private <I> void hear(TypeEncounter<I> encounter, Class<? super I> injectedClass, PreDestroyRegistry preDestroyRegistry) {
        if (injectedClass.getSuperclass() != null) {
            hear(encounter, injectedClass.getSuperclass(), preDestroyRegistry);
        }
        Method postConstructMethod = findPostConstructMethod(injectedClass);
        Method preDestroyMethod = findPreDestroyMethod(injectedClass);
        if (postConstructMethod != null) {
            register(encounter, postConstructMethod);
        }
        if (preDestroyMethod != null) {
            register(encounter, preDestroyMethod, preDestroyRegistry);
        }
    }

    private <I> Method findPostConstructMethod(Class<? super I> injectedClass) {
        List<Method> methods = Arrays.stream(injectedClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PostConstruct.class))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() != 1) {
            throw new ProvisionException("More than one @PostConstruct method found for class " + injectedClass);
        }
        Method method = methods.get(0);
        if (method.getParameterTypes().length != 0) {
            throw new ProvisionException("A method annotated with @PostConstruct must not have any parameters");
        }
        if (method.getExceptionTypes().length > 0) {
            throw new ProvisionException("A method annotated with @PostConstruct must not throw any checked exceptions");
        }
        if (Modifier.isStatic(method.getModifiers())) {
            throw new ProvisionException("A method annotated with @PostConstruct must not be static");
        }
        return method;
    }

    private <I> Method findPreDestroyMethod(Class<? super I> injectedClass) {
        List<Method> methods = Arrays.stream(injectedClass.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(PreDestroy.class))
                .collect(Collectors.toList());
        if (methods.isEmpty()) {
            return null;
        }
        if (methods.size() != 1) {
            throw new ProvisionException("More than one @PreDestroy method found for class " + injectedClass);
        }
        Method method = methods.get(0);
        if (method.getParameterTypes().length != 0) {
            throw new ProvisionException("A method annotated with @PreDestroy must not have any parameters");
        }
        return method;
    }

    private <I> void register(TypeEncounter<I> encounter, Method method) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(I injectee) {
                try {
                    method.setAccessible(true);
                    method.invoke(injectee, new Object[0]);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    if ((e.getCause() instanceof UnrecoverableException)) {
                        if (((UnrecoverableException) e.getCause()).isShowException()) {
                            LOGGER.error("An unrecoverable Exception occurred. Exiting HiveMQ", e);
                        }
                        System.exit(1);
                    }
                    throw new ProvisionException("An error occurred while calling @PostConstruct", e);
                }
            }
        });
    }

    private <I> void register(TypeEncounter<I> encounter, Method method, PreDestroyRegistry preDestroyRegistry) {
        encounter.register(new InjectionListener<I>() {
            @Override
            public void afterInjection(I injectee) {
                preDestroyRegistry.add(method, injectee);
            }
        });
    }
}
