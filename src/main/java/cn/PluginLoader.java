package cn;

import ce.IsolatedPluginClassLoader;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;
import com.hivemq.spi.PluginModule;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Iterator;

public class PluginLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginLoader.class);
    private final PluginFinder pluginFinder;

    public PluginLoader(PluginFinder pluginFinder) {
        this.pluginFinder = pluginFinder;
    }

    @ReadOnly
    public <T extends PluginModule> Collection<Class<? extends T>> load(
            @NotNull File pluginFolder, @NotNull Class<T> pluginClass) {
        Preconditions.checkNotNull(pluginClass, "plugin class must not be null");
        Preconditions.checkNotNull(pluginFolder, "plugin folder must not be null");
        Preconditions.checkArgument(pluginFolder.exists(), "%s does not exist", pluginFolder.getAbsolutePath());
        Preconditions.checkArgument(pluginFolder.canRead(), "%s is not readable", pluginFolder.getAbsolutePath());
        Preconditions.checkArgument(pluginFolder.isDirectory(), "%s is not a folder", pluginFolder.getAbsolutePath());
        Collection<URL> pluginURLs = getPluginURLs(pluginFolder);
        Collection<Class<? extends T>> modules = loadFromUrls(pluginClass, pluginURLs);
        return modules;
    }

    @ReadOnly
    public <T extends PluginModule> Collection<Class<? extends T>> loadFromUrls(
            @NotNull Class<T> desiredPluginClass, @NotNull Collection<URL> pluginURLs) {
        Preconditions.checkNotNull(desiredPluginClass, "plugin class must not be null");
        Preconditions.checkNotNull(pluginURLs, "urls must not be null");
        TypeToken<? extends T> typeToken = TypeToken.of(desiredPluginClass);
        ImmutableList.Builder<Class<? extends T>> desiredPluginsBuilder = ImmutableList.builder();
        try {
            Iterator<URL> urlIterator = pluginURLs.iterator();
            ImmutableList.Builder<Class<? extends T>> allImplementationsBuilder = ImmutableList.builder();
            while (urlIterator.hasNext()) {
                URL url = urlIterator.next();
                IsolatedPluginClassLoader pluginClassloader = new IsolatedPluginClassLoader(new URL[]{url}, getClass().getClassLoader());
                Iterable<Class<? extends T>> allPluginModuleStartingPoints = this.pluginFinder.find(desiredPluginClass, pluginClassloader);
                if (Iterables.size(allPluginModuleStartingPoints) > 1) {
                    LOGGER.warn("Plugin {} contains more than one implementation of HiveMQPluginModule.", url.toString());
                }
                allPluginModuleStartingPoints.forEach(allImplementationsBuilder::add);
            }
            allImplementationsBuilder.build().forEach(implementation -> {
                if (typeToken.getRawType().isAssignableFrom(implementation)) {
                    desiredPluginsBuilder.add(implementation);
                } else {
                    LOGGER.debug("Plugin {} is not a {} Plugin and will be ignored", implementation.getName(), typeToken.getRawType().getName());
                }
            });
        } catch (IOException | ClassNotFoundException e) {
            LOGGER.error("An error occurred while searching plugin implementations", e);
        }
        return desiredPluginsBuilder.build();
    }

    @ReadOnly
    private Collection<URL> getPluginURLs(File pluginFolder) {
        Collection<File> pluginJars = findAllPluginJars(pluginFolder);
        ImmutableList.Builder<URL> urls = ImmutableList.builder();
        pluginJars.forEach(file -> {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                LOGGER.warn("Could not add " + file.getAbsolutePath() + " to the list of files considered for plugin discovery");
                LOGGER.debug("Original exception:", e);
            }
        });
        return urls.build();
    }

    @ReadOnly
    private Collection<File> findAllPluginJars(File pluginFolder) {
        ImmutableList.Builder<File> builder = ImmutableList.builder();
        Path parentPath = FileSystems.getDefault().getPath(pluginFolder.getAbsolutePath(), new String[0]);
        try {
            DirectoryStream<Path> directoryStream = Files.newDirectoryStream(parentPath);
            Throwable throwable = null;
            try {
                directoryStream.forEach(path -> {
                    if (path.toString().endsWith("jar")) {
                        LOGGER.debug("Found plugin jar {}", path.toString());
                        builder.add(path.toFile());
                    }
                });
            } catch (Throwable t) {
                throwable = t;
                throw t;
            } finally {
                if (directoryStream != null) {
                    if (throwable != null) {
                        try {
                            directoryStream.close();
                        } catch (Throwable t) {
                            throwable.addSuppressed(t);
                        }
                    } else {
                        directoryStream.close();
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("Could not read plugins. Original exception:", e);
        }
        return builder.build();
    }
}
