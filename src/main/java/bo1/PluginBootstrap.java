package bo1;

import cn.PluginFinder;
import cn.PluginLoader;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.hivemq.spi.PluginModule;
import com.hivemq.spi.annotations.NotNull;
import com.hivemq.spi.annotations.ReadOnly;
import com.hivemq.spi.exceptions.UnrecoverableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class PluginBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(PluginBootstrap.class);
    private final PluginLoader pluginLoader;

    public PluginBootstrap() {
        this(new PluginLoader(new PluginFinder()));
    }

    @VisibleForTesting
    PluginBootstrap(PluginLoader pluginLoader) {
        this.pluginLoader = pluginLoader;
    }

    @ReadOnly
    public List<PluginModule> create(@NotNull File pluginFolder) {
        Preconditions.checkNotNull(pluginFolder, "Plugin folder must not be null");
        if (!pluginFolder.exists()) {
            LOGGER.warn("Plugin folder {} does not exist, starting without plugins", pluginFolder.getAbsolutePath());
            return Collections.emptyList();
        }
        try {
            Collection<Class<? extends PluginModule>> modules
                    = this.pluginLoader.load(pluginFolder, PluginModule.class);
            return instantiate(modules);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Error loading from plugin folder: {}", e.getMessage());
            LOGGER.debug("Original exception", e);
            LOGGER.warn("Starting without plugins", pluginFolder.getAbsolutePath());
            return Collections.emptyList();
        }
    }

    @ReadOnly
    public List<PluginModule> instantiate(@NotNull Collection<Class<? extends PluginModule>> modules) {
        Preconditions.checkNotNull(modules, "Modules must not be null");
        ImmutableList.Builder<PluginModule> builder = ImmutableList.builder();
        Iterator<Class<? extends PluginModule>> iterator = modules.iterator();
        while (iterator.hasNext()) {
            Class<? extends PluginModule> module = iterator.next();
            try {
                LOGGER.debug("Using HiveMQ Plugin Module {}", module.getName());
                PluginModule pluginModule = module.newInstance();
                builder.add(pluginModule);
            } catch (InstantiationException e) {
                LOGGER.error("Could not instantiate Plugin Module {}.", module.getName(), e);
                throw new UnrecoverableException();
            } catch (IllegalAccessException e) {
                LOGGER.error("Could not access Plugin Module {}.", module.getName(), e);
                throw new UnrecoverableException();
            } catch (Exception e) {
                if (!(e instanceof UnrecoverableException)) {
                    LOGGER.error("An unexpected exception occurred while loading plugins", e);
                }
                throw new UnrecoverableException();
            }
        }
        return builder.build();
    }
}
