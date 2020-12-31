package net.pl3x.map.util;

import java.io.File;
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
}
