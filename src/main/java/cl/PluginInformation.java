package cl;

import java.util.Objects;

public class PluginInformation {
    private final String version;
    private final String name;
    private final String description;
    private final String author;

    public PluginInformation(String version,
                             String name,
                             String description,
                             String author) {
        this.version = version;
        this.name = name;
        this.description = description;
        this.author = author;
    }

    public String getVersion() {
        return version;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getAuthor() {
        return author;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PluginInformation that = (PluginInformation) o;
        return Objects.equals(version, that.version) &&
                Objects.equals(name, that.name) &&
                Objects.equals(description, that.description) &&
                Objects.equals(author, that.author);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, name, description, author);
    }

    public String toString() {
        return "PluginInformation{version='" + this.version + '\'' + ", name='" + this.name + '\'' + ", description='" + this.description + '\'' + ", author='" + this.author + '\'' + '}';
    }
}
