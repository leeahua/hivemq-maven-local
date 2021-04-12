package cn;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

public class PluginFinder {
    private static final String META_INF_SERVICES = "META-INF/services/";

    @ReadOnly
    public <S> Iterable<Class<? extends S>> find(@NotNull Class<S> classToLoad, @NotNull ClassLoader classLoader) throws IOException, ClassNotFoundException {
        Preconditions.checkNotNull(classToLoad, "Class to load mus not be null");
        Preconditions.checkNotNull(classLoader, "Classloader must not be null");
        ImmutableList.Builder<Class<? extends S>> builder = ImmutableList.builder();
        Enumeration<URL> resources = classLoader.getResources(META_INF_SERVICES + classToLoad.getName());
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            InputStream inputStream = resource.openStream();
            Throwable throwable = null;
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
                while (true) {
                    String line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    line = handleComments(line);
                    String name = line.trim();
                    if (!name.isEmpty()) {
                        Class<?> clazz = Class.forName(name, true, classLoader);
                        builder.add(clazz.asSubclass(classToLoad));
                    }
                }
            } catch (Throwable e) {
                throwable = e;
                throw e;
            } finally {
                if (inputStream != null) {
                    if (throwable != null) {
                        try {
                            inputStream.close();
                        } catch (Throwable e) {
                            throwable.addSuppressed(e);
                        }
                    } else {
                        inputStream.close();
                    }
                }
            }
        }
        return builder.build();
    }

    private String handleComments(String line) {
        int comment = line.indexOf('#');
        if (comment >= 0) {
            line = line.substring(0, comment);
        }
        return line;
    }
}
