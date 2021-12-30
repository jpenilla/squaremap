package xyz.jpenilla.squaremap.plugin.util;

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
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;
import xyz.jpenilla.squaremap.plugin.Logging;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.config.Config;
import xyz.jpenilla.squaremap.plugin.config.Lang;

public class FileUtil {
    public static Path PLUGIN_DIR;
    public static Path WEB_DIR;
    public static Path LOCALE_DIR;
    public static Path TILES_DIR;

    static {
        reload();
    }

    public static void reload() {
        PLUGIN_DIR = SquaremapPlugin.getInstance().getDataFolder().toPath();
        WEB_DIR = PLUGIN_DIR.resolve(Config.WEB_DIR);
        LOCALE_DIR = PLUGIN_DIR.resolve("locale");
        TILES_DIR = WEB_DIR.resolve("tiles");
    }

    private static Path getRegionFolder(World world) {
        return LevelStorageSource
            .getStorageFolder(
                world.getWorldFolder().toPath(),
                ((CraftWorld) world).getHandle().getTypeKey()
            )
            .resolve("region");
    }

    public static Path[] getRegionFiles(World world) {
        final Path regionFolder = getRegionFolder(world);
        try (final Stream<Path> stream = Files.list(regionFolder)) {
            return stream.filter(it -> it.getFileName().toString().endsWith(".mca")).toArray(Path[]::new);
        } catch (final IOException ex) {
            throw new RuntimeException("Failed to list region files in " + regionFolder.toAbsolutePath(), ex);
        }
    }

    public static Path getAndCreateTilesDirectory(World world) {
        final Path dir = TILES_DIR.resolve(world.getName());
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (IOException e) {
                Logging.severe(Lang.LOG_COULD_NOT_CREATE_DIR.replace("{path}", dir.toAbsolutePath().toString()), e);
            }
        }
        return dir;
    }

    public static void deleteSubdirectories(Path dir) throws IOException {
        try (Stream<Path> files = Files.list(dir)) {
            files.forEach(path -> {
                try {
                    deleteDirectory(path);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public static void deleteDirectory(Path dir) throws IOException {
        try (Stream<Path> walk = Files.walk(dir)) {
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

        Logging.debug("Extracting " + inDir + " directory from jar...");
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
                    Logging.debug("  exists   " + name);
                    continue;
                }
                if (entry.isDirectory()) {
                    if (!file.exists()) {
                        final boolean result = file.mkdir();
                        Logging.debug((result ? "  creating " : "  unable to create ") + name);
                    } else {
                        Logging.debug("  exists   " + name);
                    }
                } else {
                    Logging.debug("  writing  " + name);
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
                        Logging.severe("Failed to extract file (" + name + ") from jar!", e);
                    }
                }
            }
        } catch (IOException e) {
            Logging.severe("Failed to extract directory '" + inDir + "' from jar to '" + outDir + "'", e);
        }
    }

    public static void write(String str, Path file) {
        ForkJoinPool.commonPool().execute(() -> {
            try {
                replaceFile(file, str);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void replaceFile(Path path, String str) throws IOException {
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
