package net.pl3x.map.plugin.util;

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
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.pl3x.map.plugin.Logging;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_18_R1.CraftWorld;

public class FileUtil {
    public static Path PLUGIN_DIR = Pl3xMapPlugin.getInstance().getDataFolder().toPath();
    public static Path WEB_DIR = PLUGIN_DIR.resolve(Config.WEB_DIR);
    public static Path LOCALE_DIR = PLUGIN_DIR.resolve("locale");
    public static Path TILES_DIR = WEB_DIR.resolve("tiles");
    public static final Map<UUID, Path> WORLD_DIRS = new HashMap<>();
    public static final Map<UUID, Path> REGION_DIRS = new HashMap<>();

    private static FileSystem fileSystem;

    public static void reload() {
        PLUGIN_DIR = Pl3xMapPlugin.getInstance().getDataFolder().toPath();
        WEB_DIR = PLUGIN_DIR.resolve(Config.WEB_DIR);
        TILES_DIR = WEB_DIR.resolve("tiles");

        WORLD_DIRS.clear();
        REGION_DIRS.clear();
    }

    public static Path getRegionFolder(World world) {
        Path dir = REGION_DIRS.get(world.getUID());
        if (dir == null) {
            dir = LevelStorageSource.getStorageFolder(world.getWorldFolder().toPath(), ((CraftWorld) world).getHandle().getTypeKey())
                    .resolve("region");
            REGION_DIRS.put(world.getUID(), dir);
        }
        return dir;
    }

    public static File[] getRegionFiles(World world) {
        File[] files = getRegionFolder(world).toFile().listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) {
            files = new File[0];
        }
        return files;
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

    public static Path getWorldFolder(World world) {
        Path dir = WORLD_DIRS.get(world.getUID());
        if (dir == null) {
            dir = TILES_DIR.resolve(world.getName());
            try {
                Files.createDirectories(dir);
                WORLD_DIRS.put(world.getUID(), dir);
            } catch (IOException e) {
                Logging.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                        .replace("{path}", dir.toAbsolutePath().toString()), e);
            }
        }
        return dir;
    }

    public static void extract(String inDir, File outDir, boolean replace) {
        // https://coderanch.com/t/472574/java/extract-directory-current-jar
        final URL dirURL = FileUtil.class.getResource(inDir);
        final String path = inDir.substring(1);
        if ((dirURL != null) && dirURL.getProtocol().equals("jar")) {
            ZipFile jar;
            try {
                Logging.debug("Extracting " + inDir + " directory from jar...");
                jar = ((JarURLConnection) dirURL.openConnection()).getJarFile();
            } catch (IOException e) {
                Logging.severe("Failed to extract directory from jar", e);
                return;
            }
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
                    Logging.debug("  <yellow>exists</yellow>   " + name);
                    continue;
                }
                if (entry.isDirectory()) {
                    if (!file.exists()) {
                        final boolean result = file.mkdir();
                        Logging.debug((result ? "  <green>creating</green> " : "  <red>unable to create</red> ") + name);
                    } else {
                        Logging.debug("  <yellow>exists</yellow>   " + name);
                    }
                } else {
                    Logging.debug("  <green>writing</green>  " + name);
                    try {
                        final InputStream inputStream = jar.getInputStream(entry);
                        final OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(file));
                        final byte[] buffer = new byte[4096];
                        int readCount;
                        while ((readCount = inputStream.read(buffer)) > 0) {
                            outputStream.write(buffer, 0, readCount);
                        }
                        outputStream.close();
                        inputStream.close();
                    } catch (IOException e) {
                        Logging.severe("Failed to extract file (" + name + ") from jar!", e);
                    }
                }
            }
        } else if (dirURL == null) {
            throw new IllegalStateException("can't find " + inDir + " on the classpath");
        } else {
            throw new IllegalStateException("don't know how to handle extracting from " + dirURL);
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
