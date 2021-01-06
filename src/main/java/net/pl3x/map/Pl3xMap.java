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

        this.updatePlayers = new UpdatePlayers();
        this.updatePlayers.runTaskTimer(this, 20, 20);
        this.updateWorldData = new UpdateWorldData();
        this.updateWorldData.runTaskTimer(this, 0, 20 * 5);

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

        if (this.updatePlayers != null && !this.updatePlayers.isCancelled()) {
            this.updatePlayers.cancel();
            this.updatePlayers = null;
        }
        if (this.updateWorldData != null && !this.updateWorldData.isCancelled()) {
            this.updateWorldData.cancel();
            this.updateWorldData = null;
        }

        RenderManager.stop();

        getServer().getScheduler().cancelTasks(this);
    }
}
