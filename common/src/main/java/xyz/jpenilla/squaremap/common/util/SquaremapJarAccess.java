package xyz.jpenilla.squaremap.common.util;

import com.google.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;

@DefaultQualifier(NonNull.class)
public interface SquaremapJarAccess {
    void useJar(CheckedConsumer<Path, IOException> consumer) throws IOException, URISyntaxException;

    default void extract(final String inDir, final Path outDir, final boolean replaceExisting) {
        try {
            this.useJar(root -> FileUtil.specialCopyRecursively(root.resolve(inDir), outDir, replaceExisting));
        } catch (final IOException | URISyntaxException ex) {
            Logging.logger().error("Failed to extract directory '{}' from jar to '{}'", inDir, outDir, ex);
        }
    }

    final class JarFromCodeSource implements SquaremapJarAccess {
        @Inject
        private JarFromCodeSource() {
        }

        @Override
        public void useJar(final CheckedConsumer<Path, IOException> consumer) throws IOException, URISyntaxException {
            FileUtil.openJar(jar(), fileSystem -> consumer.accept(fileSystem.getPath("/")));
        }

        private static Path jar() throws URISyntaxException, IOException {
            URL sourceUrl = JarFromCodeSource.class.getProtectionDomain().getCodeSource().getLocation();
            // Some class loaders give the full url to the class, some give the URL to its jar.
            // We want the containing jar, so we will unwrap jar-schema code sources.
            if (sourceUrl.getProtocol().equals("jar")) {
                final int exclamationIdx = sourceUrl.getPath().lastIndexOf('!');
                if (exclamationIdx != -1) {
                    sourceUrl = new URL(sourceUrl.getPath().substring(0, exclamationIdx));
                }
            }
            return Paths.get(sourceUrl.toURI());
        }
    }
}
