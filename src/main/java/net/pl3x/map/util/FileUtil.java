package net.pl3x.map.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.pl3x.map.Logger;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import org.bukkit.World;

public class FileUtil {
    private static File webDir;
    private static File tilesDir;
    private static final Map<UUID, File> worldDirs = new HashMap<>();
    private static final Map<UUID, File> regionDirs = new HashMap<>();
    private static FileSystem fileSystem;

    public static void reload() {
        webDir = null;
        tilesDir = null;
        worldDirs.clear();
        regionDirs.clear();
    }

    public static File getRegionFolder(World world) {
        File dir = regionDirs.get(world.getUID());
        if (dir == null) {
            switch (world.getEnvironment()) {
                case NETHER:
                    dir = new File(new File(world.getWorldFolder(), "DIM-1"), "region");
                    break;
                case THE_END:
                    dir = new File(new File(world.getWorldFolder(), "DIM1"), "region");
                    break;
                case NORMAL:
                default:
                    dir = new File(world.getWorldFolder(), "region");
            }
        }
        return dir;
    }

    public static File[] getRegionFiles(World world) {
        File[] files = FileUtil.getRegionFolder(world).listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) {
            files = new File[0];
        }
        return files;
    }

    public static File getPluginFolder() {
        return Pl3xMap.getInstance().getDataFolder();
    }

    public static File getWebFolder() {
        if (webDir == null) {
            File dir = new File(Config.WEB_DIR);
            if (dir.isAbsolute()) {
                webDir = dir;
            } else {
                webDir = mkdirs(new File(getPluginFolder(), Config.WEB_DIR));
            }
        }
        return webDir;
    }

    public static File getTilesFolder() {
        if (tilesDir == null) {
            tilesDir = mkdirs(new File(FileUtil.getWebFolder(), "tiles"));
        }
        return tilesDir;
    }

    public static File getWorldFolder(World world) {
        File dir = worldDirs.get(world.getUID());
        if (dir == null) {
            dir = mkdirs(new File(getTilesFolder(), world.getName()));
            if (dir != null) {
                worldDirs.put(world.getUID(), dir);
            }
        }
        return dir;
    }

    public static File mkdirs(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Logger.severe(Lang.LOG_COULD_NOT_CREATE_DIR
                        .replace("{path}", dir.getAbsolutePath()));
                return null;
            }
        }
        return dir;
    }

    public static void extractWebFolder() {
        try {
            copyFromJar("web", getWebFolder().toPath());
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    public static void copyFromJar(String source, final Path destination) throws URISyntaxException, IOException {
        if (fileSystem == null) {
            fileSystem = FileSystems.newFileSystem(Pl3xMap.getInstance().getClass().getResource("").toURI(), Collections.emptyMap());
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

    public static boolean deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                deleteDirectory(file);
            }
        }
        return dir.delete();
    }

    public static void writeStringToFile(String str, File file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            writer.write(str);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
