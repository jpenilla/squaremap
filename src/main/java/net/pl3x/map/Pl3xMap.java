package net.pl3x.map;

import java.util.logging.Logger;
import net.pl3x.map.command.CmdPl3xMap;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pl3xMap extends JavaPlugin {
    private static Pl3xMap instance;

    public Pl3xMap() {
        instance = this;
    }

    @Override
    public void onEnable() {
        getCommand("pl3xmap").setExecutor(new CmdPl3xMap());
    }

    public static Pl3xMap getInstance() {
        return instance;
    }

    public static Logger log() {
        return getInstance().getLogger();
    }
}
