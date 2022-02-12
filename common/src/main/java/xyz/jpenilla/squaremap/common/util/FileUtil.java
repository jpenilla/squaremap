package xyz.jpenilla.squaremap.common.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.server.level.ServerLevel;
import xyz.jpenilla.squaremap.common.Logging;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.config.Config;
import xyz.jpenilla.squaremap.common.config.Lang;

public final class FileUtil {
    public static Path PLUGIN_DIR;
    public static Path WEB_DIR;
    public static Path LOCALE_DIR;
    public static Path TILES_DIR;

    static {
        reload();
    }

    public static void reload() {
        PLUGIN_DIR = SquaremapCommon.instance().platform().dataDirectory();
        WEB_DIR = PLUGIN_DIR.resolve(Config.WEB_DIR);
        LOCALE_DIR = PLUGIN_DIR.resolve("locale");
        TILES_DIR = WEB_DIR.resolve("tiles");
    }

    public static Path[] getRegionFiles(final ServerLevel level) {
        final Path regionFolder = SquaremapCommon.instance().platform().regionFileDirectory(level);
        Logging.debug(() -> "Listing region files for directory '" + regionFolder + "'...");
        try (final Stream<Path> stream = Files.list(regionFolder)) {
            return stream.filter(file -> file.getFileName().toString().endsWith(".mca")).toArray(Path[]::new);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to list region files in " + regionFolder.toAbsolutePath(), ex);
        }
    }

    public static Path getAndCreateTilesDirectory(final ServerLevel level) {
        final Path dir = TILES_DIR.resolve(Util.levelWebName(level));
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (final IOException e) {
                Logging.error(Lang.LOG_COULD_NOT_CREATE_DIR, e, "path", dir.toAbsolutePath());
            }
        }
        return dir;
    }

    public static void deleteSubdirectories(Path dir) throws IOException {
        try (final Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                try {
                    deleteDirectory(path);
                } catch (final IOException e) {
                    Logging.logger().warn("Failed to delete directory {}", path, e);
                }
            });
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        try (final Stream<Path> walk = Files.walk(dir)) {
            //noinspection ResultOfMethodCallIgnored
            walk.sorted(Comparator.reverseOrder())
                .map(Path::toFile)
                .forEach(File::delete);
        }
    }

    public static void extract(String inDir, File outDir, boolean replace) {
        // https://coderanch.com/t/472574/java/extract-directory-current-jar
        final URL dirURL = FileUtil.class.getResource(inDir);
        final String path = inDir.substring(1);

        if (dirURL == null) {
            throw new IllegalStateException("can't find " + inDir + " on the classpath");
        } else if (!dirURL.getProtocol().equals("jar")) {
            throw new IllegalStateException("don't know how to handle extracting from " + dirURL);
        }

        Logging.debug(() -> "Extracting " + inDir + " directory from jar...");
        try (final ZipFile jar = ((JarURLConnection) dirURL.openConnection()).getJarFile()) {
            final Enumeration<? extends ZipEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                final ZipEntry entry = entries.nextElement();
                final String name = entry.getName();
                if (!name.startsWith(path)) {
                    continue;
                }
                final String filename = name.substring(path.length());
                final File file = new File(outDir, filename);
                if (!replace && file.exists()) {
                    Logging.debug(() -> "  exists   " + name);
                    continue;
                }
                if (entry.isDirectory()) {
                    if (!file.exists()) {
                        final boolean result = file.mkdir();
                        Logging.debug(() -> (result ? "  creating " : "  unable to create ") + name);
                    } else {
                        Logging.debug(() -> "  exists   " + name);
                    }
                } else {
                    Logging.debug(() -> "  writing  " + name);
                    try (
                        final InputStream inputStream = jar.getInputStream(entry);
                        final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file))
                    ) {
                        final byte[] buffer = new byte[4096];
                        int readCount;
                        while ((readCount = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, readCount);
                        }
                    } catch (IOException e) {
                        Logging.logger().error("Failed to extract file ({}) from jar!", name, e);
                    }
                }
            }
        } catch (IOException e) {
            Logging.logger().error("Failed to extract directory '{}' from jar to '{}'", inDir, outDir, e);
        }
    }

    public static void writeString(final Path file, final Supplier<String> string) {
        ForkJoinPool.commonPool().execute(() -> writeString0(file, string.get()));
    }

    private static void writeString0(final Path path, final String string) {
        try {
            replaceFile(path, string);
        } catch (final IOException ex) {
            Logging.logger().warn("Failed to write file {}", path, ex);
        }
    }

    private static void replaceFile(final Path path, final String str) throws IOException {
        final Path tmp = path.resolveSibling("." + path.getFileName().toString() + ".tmp");

        try {
            Files.writeString(tmp, str, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
            }
            throw e;
        }

        try {
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AccessDeniedException | AtomicMoveNotSupportedException e) {
            try {
                Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING);
            } catch (NoSuchFileException | AccessDeniedException ignore) {
            }
        } catch (NoSuchFileException ignore) {
        }
    }
}
