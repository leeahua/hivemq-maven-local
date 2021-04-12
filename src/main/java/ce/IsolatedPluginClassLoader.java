package ce;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class IsolatedPluginClassLoader extends URLClassLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(IsolatedPluginClassLoader.class);
    private ClassLoader classLoader;

    public IsolatedPluginClassLoader(URL[] urls, ClassLoader classLoader) {
        super(urls, classLoader);
        this.classLoader = classLoader;
    }

    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        LOGGER.trace("Checking if class {} has already been loaded...", name);
        Class loadedClass = findLoadedClass(name);
        if (loadedClass == null) {
            LOGGER.trace("Class {} has not been loaded yet", name);
            if (isRestrictedPackages(name)) {
                try {
                    loadedClass = super.loadClass(name, resolve);
                } catch (ClassNotFoundException e) {
                    LOGGER.trace("Class {} was not found in parent classpath, trying to find it in local classloader {}", name, this);
                    loadedClass = findClass(name);
                }
            } else {
                try {
                    loadedClass = findClass(name);
                    LOGGER.trace("Found class {} in plugin classpath", name);
                } catch (ClassNotFoundException e) {
                    LOGGER.trace("Class {} was not found in local classpath, trying to find it in parent classloader {}", name, this.classLoader);
                    loadedClass = super.loadClass(name, resolve);
                }
            }
        } else {
            LOGGER.trace("Class {} has already been loaded", name);
        }
        if (resolve) {
            resolveClass(loadedClass);
        }
        return loadedClass;
    }


    private boolean isRestrictedPackages(String name) {
        Set<String> restrictedPackages = new HashSet<>();
        restrictedPackages.add("java.");
        restrictedPackages.add("com.hivemq.spi");
        restrictedPackages.add("com.google.inject");
        restrictedPackages.add("javax.inject");
        restrictedPackages.add("javax.annotation");
        restrictedPackages.add("org.slf4j");
        restrictedPackages.add("com.codahale.metrics");
        restrictedPackages.add("javax.ws.rs");
        restrictedPackages.add("javax.servlet");
        return restrictedPackages.stream()
                .anyMatch(name::startsWith);
    }

    public URL getResource(String name) {
        URL resource = findResource(name);
        if (resource == null) {
            resource = super.getResource(name);
        }
        return resource;
    }

    public Enumeration<URL> getResources(String name) throws IOException {
        Enumeration<URL> localResources = findResources(name);
        Enumeration<URL> parentResources = null;
        if (getParent() != null) {
            parentResources = getParent().getResources(name);
        }
        List<URL> resources = new ArrayList<>();
        if (localResources != null) {
            while (localResources.hasMoreElements()) {
                resources.add(localResources.nextElement());
            }
        }
        if (parentResources != null) {
            while (parentResources.hasMoreElements()) {
                resources.add(parentResources.nextElement());
            }
        }
        return new Enumeration<URL>() {
            private Iterator<URL> urlIterator = resources.iterator();

            @Override
            public boolean hasMoreElements() {
                return urlIterator.hasNext();
            }

            @Override
            public URL nextElement() {
                return urlIterator.next();
            }
        };
    }

    public InputStream getResourceAsStream(String name) {
        URL resource = getResource(name);
        try {
            return resource != null ? resource.openStream() : null;
        } catch (IOException e) {
        }
        return null;
    }
}
