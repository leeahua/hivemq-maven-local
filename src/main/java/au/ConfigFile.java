package au;

import com.hivemq.spi.annotations.Nullable;

import java.io.File;
import java.util.Optional;

public class ConfigFile {
    private final Optional<File> file;

    public ConfigFile(@Nullable File file) {
        this.file = Optional.ofNullable(file);
    }

    public Optional<File> get() {
        return this.file;
    }
}
