package net.pl3x.map;

import net.pl3x.map.command.CommandManager;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.task.UpdatePlayers;
import net.pl3x.map.task.UpdateWorldData;
import net.pl3x.map.util.FileUtil;
import net.pl3x.map.util.IntegratedServer;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

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

        try {
            new CommandManager(this);
        } catch (Exception e) {
            getLogger().log(Level.WARNING, "Failed to initialize command manager", e);
            this.setEnabled(false);
            return;
        }

        start();
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

        WorldManager.start();

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

        WorldManager.shutdown();

        getServer().getScheduler().cancelTasks(this);
    }
}
