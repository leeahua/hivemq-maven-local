package cb1;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class ManifestUtils {
    public static String attribute(Class clazz, String attribute) {
        try {
            URLClassLoader urlClassLoader = (URLClassLoader) clazz.getClassLoader();
            URL url = urlClassLoader.findResource("META-INF/MANIFEST.MF");
            Manifest manifest = new Manifest(url.openStream());
            Attributes attributes = manifest.getMainAttributes();
            return attributes.getValue(attribute);
        } catch (IOException | ClassCastException e) {
        }
        return null;
    }
}
