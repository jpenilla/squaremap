package xyz.jpenilla.squaremap.common.util;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.CopyOption;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.Logging;

@DefaultQualifier(NonNull.class)
public final class FileUtil {
    private FileUtil() {
    }

    private static final ExecutorService writeExecutor = Executors.newSingleThreadExecutor();

    public static void deleteContentsRecursively(final Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            return;
        }
        try (final Stream<Path> files = Files.list(directory)) {
            files.forEach(Util.sneaky(FileUtil::deleteRecursively));
        }
    }

    public static void deleteRecursively(final Path path) throws IOException {
        if (!Files.exists(path)) {
            return;
        }
        try (final Stream<Path> stream = Files.walk(path)) {
            // Reverse order: visit files before directories
            stream.sorted(Comparator.reverseOrder()).forEach(Util.sneaky(Files::delete));
        }
    }

    public static void openJar(final Path jar, final CheckedConsumer<FileSystem, IOException> consumer) throws IOException {
        try (final FileSystem fileSystem = FileSystems.newFileSystem(jar)) {
            consumer.accept(fileSystem);
        }
    }

    /**
     * Special recursive copy function which will silently ignore existing files
     * when {@code replaceExisting} is false instead of throwing an exception as
     * might be expected.
     *
     * <p>When {@code replaceExisting} is true and a directory is encountered in the
     * place where we are trying to extract a file, the directory will be deleted
     * via {@link #deleteRecursively(Path)} before the copy as
     * {@link StandardCopyOption#REPLACE_EXISTING} only handles replacing files.</p>
     *
     * @param from            source directory
     * @param to              destination directory
     * @param replaceExisting whether to replace existing files and directories
     * @throws IOException if an I/O error occurs
     */
    public static void specialCopyRecursively(
        final Path from,
        final Path to,
        final boolean replaceExisting
    ) throws IOException {
        if (!Files.exists(from)) {
            return;
        }
        if (!Files.exists(to)) {
            Files.createDirectories(to);
        }
        try (final Stream<Path> stream = Files.walk(from)) {
            stream.forEach(Util.sneaky(path -> {
                final Path target = to.resolve(invariantSeparatorsPathString(from.relativize(path)));
                if (Files.isDirectory(path)) {
                    if (Files.isDirectory(target)) {
                        return;
                    }
                    if (Files.exists(target)) {
                        if (replaceExisting) {
                            Files.delete(target);
                        } else {
                            return;
                        }
                    }
                    Files.createDirectories(target);
                } else {
                    if (!replaceExisting && Files.exists(target)) {
                        return;
                    }
                    if (replaceExisting && Files.isDirectory(target)) {
                        deleteRecursively(target);
                    }
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }));
        }
    }

    public static String invariantSeparatorsPathString(final Path path) {
        final String separator = path.getFileSystem().getSeparator();
        final String pathString = path.toString();
        return separator.equals("/")
            ? pathString
            : pathString.replace(separator, "/");
    }

    public static void atomicWriteJsonAsync(final Path file, final Object object) {
        atomicWriteAsync(file, tmp -> {
            try (final BufferedWriter writer = Files.newBufferedWriter(tmp)) {
                Util.gson().toJson(object, writer);
            }
        });
    }

    public static void atomicWriteAsync(final Path file, final CheckedConsumer<Path, IOException> op) {
        writeExecutor.execute(() -> {
            try {
                atomicWrite(file, op);
            } catch (final IOException ex) {
                Logging.logger().warn("Failed to write file '{}'", file, ex);
            }
        });
    }

    public static void atomicWrite(final Path path, final CheckedConsumer<Path, IOException> op) throws IOException {
        final Path tmp = siblingTempFile(path);

        try {
            op.accept(tmp);
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

    private static Path siblingTempFile(final Path path) {
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

            public static void shutdownExecutor() {
                writeExecutor.shutdown();
                try {
                    if (!writeExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                        writeExecutor.shutdownNow();
                        if (!writeExecutor.awaitTermination(60, TimeUnit.SECONDS))
                            System.err.println("ExecutorService did not terminate");
                    }
                } catch (InterruptedException ie) {
                    writeExecutor.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
        }
