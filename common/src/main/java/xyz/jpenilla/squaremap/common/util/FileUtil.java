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
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadLocalRandom;
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

    public static void deleteContentsRecursively(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (final Stream<Path> files = Files.list(directory)) {
            files.forEach(path -> {
                try {
                    deleteRecursively(path);
                } catch (final IOException ex) {
                    Util.rethrow(ex);
                }
            });
        }
    }

    public static void deleteRecursively(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (final Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder())
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (final IOException ex) {
                        Util.rethrow(ex);
                    }
                });
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
                        Logging.logger().error("Failed to extract file '{}' from jar!", name, e);
                    }
                }
            }
        } catch (IOException e) {
            Logging.logger().error("Failed to extract directory '{}' from jar to '{}'", inDir, outDir, e);
        }
    }

    public static void writeString(final Path file, final Supplier<String> string) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                writeString(file, string.get());
            } catch (final IOException ex) {
                Logging.logger().warn("Failed to write file '{}'", file, ex);
            }
        });
    }

    private static void writeString(final Path path, final String str) throws IOException {
        final Path tmp = siblingTempFile(path);

        try {
            Files.writeString(tmp, str);
            atomicMove(tmp, path, true);
        } catch (final IOException ex) {
            try {
                Files.deleteIfExists(tmp);
            } catch (final IOException ex1) {
                ex.addSuppressed(ex1);
            }
            throw ex;
        }
    }

    public static Path siblingTempFile(final Path path) {
        return path.resolveSibling("." + System.nanoTime() + "-" + ThreadLocalRandom.current().nextInt() + "-" + path.getFileName().toString() + ".tmp");
    }

    @SuppressWarnings("BusyWait") // not busy waiting
    public static void atomicMove(final Path from, final Path to, final boolean replaceExisting) throws IOException {
        final int maxRetries = 2;

        try {
            atomicMoveIfPossible(from, to, replaceExisting);
        } catch (final AccessDeniedException ex) {
            // Sometimes because of file locking this will fail... Let's just try again and hope for the best
            // Thanks Windows!
            int retries = 1;
            while (true) {
                try {
                    // Pause for a bit
                    Thread.sleep(10L * retries);
                    atomicMoveIfPossible(from, to, replaceExisting);
                    break; // success
                } catch (final AccessDeniedException ex1) {
                    ex.addSuppressed(ex1);
                    if (retries == maxRetries) {
                        throw ex;
                    }
                } catch (final InterruptedException interruptedException) {
                    ex.addSuppressed(interruptedException);
                    Thread.currentThread().interrupt();
                    throw ex;
                }
                ++retries;
            }
        }
    }

    private static void atomicMoveIfPossible(final Path from, final Path to, final boolean replaceExisting) throws IOException {
        final CopyOption[] options = replaceExisting
            ? new CopyOption[]{StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING}
            : new CopyOption[]{StandardCopyOption.ATOMIC_MOVE};

        try {
            Files.move(from, to, options);
        } catch (final AtomicMoveNotSupportedException ex) {
            Files.move(from, to, replaceExisting ? new CopyOption[]{StandardCopyOption.REPLACE_EXISTING} : new CopyOption[]{});
        }
    }
}
