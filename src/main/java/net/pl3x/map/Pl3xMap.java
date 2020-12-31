package net.pl3x.map;

import fi.iki.elonen.NanoHTTPD;
import java.io.IOException;
import net.pl3x.map.command.CmdPl3xMap;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pl3xMap extends JavaPlugin {
    private static Pl3xMap instance;

    public Pl3xMap() {
        instance = this;
    }

    @Override
    public void onEnable() {
        Config.reload();
        Lang.reload();

        FileUtil.extractWebFolder();

        Httpd.startServer();

        PluginCommand cmd = getCommand("pl3xmap");
        if (cmd != null) {
            cmd.setExecutor(new CmdPl3xMap());
        }
    }

    @Override
    public void onDisable() {
        Httpd.stopServer();
    }

    public static Pl3xMap getInstance() {
        return instance;
    }
}
