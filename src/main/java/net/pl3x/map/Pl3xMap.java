package net.pl3x.map;

import net.pl3x.map.command.CmdPl3xMap;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.task.UpdatePlayers;
import net.pl3x.map.task.UpdateWorldData;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.IntegratedServer;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class Pl3xMap extends JavaPlugin {
    private static Pl3xMap instance;
    private UpdateWorldData updateWorldData;
    private UpdatePlayers updatePlayers;

    public Pl3xMap() {
        instance = this;
    }

    @Override
    public void onEnable() {
        Config.reload();
        Lang.reload();

        start();

        PluginCommand cmd = getCommand("pl3xmap");
        if (cmd != null) {
            cmd.setExecutor(new CmdPl3xMap());
        }
    }

    @Override
    public void onDisable() {
        stop();
    }

    public static Pl3xMap getInstance() {
        return instance;
    }

    public void start() {
        FileUtil.extractWebFolder();

        new UpdatePlayers().runTaskTimer(this, 20, 20);
        new UpdateWorldData().runTaskTimer(this, 0, 20 * 5);

        if (Config.HTTPD_ENABLED) {
            if (IntegratedServer.setup()) {
                IntegratedServer.startServer();
            }
        }
    }

    public void stop() {
        if (Config.HTTPD_ENABLED) {
            IntegratedServer.stopServer();
        }

        getServer().getScheduler().cancelTasks(this);
    }
}
