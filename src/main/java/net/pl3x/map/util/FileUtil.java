package net.pl3x.map.util;

import java.io.File;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.configuration.Config;
import org.bukkit.World;

public class FileUtil {
    public static File getRegionFolder(World world) {
        switch (world.getEnvironment()) {
            case NETHER:
                return new File(new File(world.getWorldFolder(), "DIM-1"), "region");
            case THE_END:
                return new File(new File(world.getWorldFolder(), "DIM1"), "region");
            case NORMAL:
            default:
                return new File(world.getWorldFolder(), "region");
        }
    }

    public static File[] getRegionFiles(World world) {
        File[] files = FileUtil.getRegionFolder(world).listFiles((dir, name) -> name.endsWith(".mca"));
        if (files == null) {
            files = new File[0];
        }
        return files;
    }

    public static File getWebFolder() {
        return new File(Pl3xMap.getInstance().getDataFolder(), Config.WEB_DIR);
    }

    public static void extractWebFolder() {
        if (!new File(getWebFolder(), "index.html").exists()) {
            Pl3xMap.getInstance().saveResource("web/index.html", true);
        }
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
}
