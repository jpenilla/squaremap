package xyz.jpenilla.squaremap.common.util;

import com.google.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.jar.Manifest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;

@DefaultQualifier(NonNull.class)
public interface SquaremapJarAccess {
    /**
     * Provides a path within squaremap's packaged files.
     *
     * @param path the path, relative to the root of the packaged files
     * @param consumer receives the path
     */
    void usePath(String path, CheckedConsumer<Path, IOException> consumer) throws IOException, URISyntaxException;

    static @Nullable Manifest manifest(final Class<?> clazz) {
        final String classLocation = "/" + clazz.getName().replace(".", "/") + ".class";
        final @Nullable URL resource = clazz.getResource(classLocation);
        if (resource == null) {
            return null;
        }
        final String classFilePath = resource.toString().replace("\\", "/");
        final String archivePath = classFilePath.substring(0, classFilePath.length() - classLocation.length());
        try (final InputStream stream = URI.create(archivePath + "/META-INF/MANIFEST.MF").toURL().openStream()) {
            return new Manifest(stream);
        } catch (final IOException ex) {
            return null;
        }
    }

    default void extract(final String inDir, final Path outDir, final boolean replaceExisting) {
        try {
            this.usePath(inDir, source -> FileUtil.specialCopyRecursively(source, outDir, replaceExisting));
        } catch (final IOException | URISyntaxException ex) {
            Logging.logger().error("Failed to extract directory '{}' from jar to '{}'", inDir, outDir, ex);
        }
    }

    final class JarFromCodeSource implements SquaremapJarAccess {
        @Inject
        private JarFromCodeSource() {
        }

        @Override
        public void usePath(final String path, final CheckedConsumer<Path, IOException> consumer) throws IOException, URISyntaxException {
            FileUtil.openJar(jar(), fileSystem -> consumer.accept(fileSystem.getPath("/", path)));
        }

        private static Path jar() throws URISyntaxException, IOException {
            URL sourceUrl = JarFromCodeSource.class.getProtectionDomain().getCodeSource().getLocation();
            // Some class loaders give the full url to the class, some give the URL to its jar.
            // We want the containing jar, so we will unwrap jar-schema code sources.
            if (sourceUrl.getProtocol().equals("jar")) {
                final int exclamationIdx = sourceUrl.getPath().lastIndexOf('!');
                if (exclamationIdx != -1) {
                    sourceUrl = URI.create(sourceUrl.getPath().substring(0, exclamationIdx)).toURL();
                }
            }
            return Paths.get(sourceUrl.toURI());
        }
    }
}
