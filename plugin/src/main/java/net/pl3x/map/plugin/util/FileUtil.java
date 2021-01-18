package net.pl3x.map.plugin.util;

import net.pl3x.map.plugin.Logger;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import org.bukkit.World;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Stream;

public class FileUtil {
    public static Path PLUGIN_DIR = Pl3xMapPlugin.getInstance().getDataFolder().toPath();
    public static Path WEB_DIR = PLUGIN_DIR.resolve(Config.WEB_DIR);
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
            switch (world.getEnvironment()) {
                case NETHER:
                    dir = Path.of(world.getWorldFolder().getAbsolutePath(), "DIM-1", "region");
                    break;
                case THE_END:
                    dir = Path.of(world.getWorldFolder().getAbsolutePath(), "DIM1", "region");
                    break;
                case NORMAL:
                default:
                    dir = Path.of(world.getWorldFolder().getAbsolutePath(), "region");
            }
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
                Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                        .replace("{path}", dir.toAbsolutePath().toString()));
                e.printStackTrace();
            }
        }
        return dir;
    }

    public static void extractWebFolder() {
        try {
            copyFromJar("web", WEB_DIR);
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFromJar(String source, final Path destination) throws URISyntaxException, IOException {
        if (fileSystem == null) {
            fileSystem = FileSystems.newFileSystem(Pl3xMapPlugin.getInstance().getClass().getResource("").toURI(), Collections.emptyMap());
        }
        final Path jarPath = fileSystem.getPath(source);
        Files.walkFileTree(jarPath, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(destination.resolve(jarPath.relativize(dir).toString()));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path target = destination.resolve(jarPath.relativize(file).toString());
                if (Config.UPDATE_WEB_DIR) {
                    Files.copy(file, target, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    try {
                        Files.copy(file, target);
                    } catch (FileAlreadyExistsException ignore) {
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });
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
            Files.write(tmp, str.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
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
            } catch (AccessDeniedException ignore) {
            }
        }
    }
}
