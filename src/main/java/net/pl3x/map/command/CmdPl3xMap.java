package net.pl3x.map.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;

public class CmdPl3xMap {
    private final Pl3xMap plugin;

    public CmdPl3xMap(Pl3xMap plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Starts a full render")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap|map fullrender <world>")
    private void onFullRender(
            CommandSender sender,
            @Argument("world") World world
    ) {
        if (RenderManager.isRendering(world)) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS
                    .replace("{world}", world.getName()));
            return;
        }

        Lang.send(sender, Lang.FULL_RENDER_STARTED
                .replace("{world}", world.getName()));
        RenderManager.fullRender(world);
    }

    @CommandDescription("Starts a radius render")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap|map radiusrender <world> <radius> <x> <z>")
    private void onRadiusRender(
            CommandSender sender,
            @Argument("world") World world,
            @Argument("radius") int radius,
            @Argument("x") int x,
            @Argument("z") int z
    ) {
        Lang.send(sender, "Not implemented yet");
    }

    @CommandDescription("Cancels a render")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap|map cancelrender <world>")
    private void onCancelRender(
            CommandSender sender,
            @Argument("world") World world
    ) {
        if (!RenderManager.isRendering(world)) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS
                    .replace("{world}", world.getName()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER
                .replace("{world}", world.getName()));
        RenderManager.cancelRender(world);
    }

    @CommandDescription("Reloads the plugin")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap|map reload")
    private void onReload(CommandSender sender) {
        plugin.stop();

        Config.reload();
        Lang.reload();
        FileUtil.reload();

        plugin.start();

        PluginDescriptionFile desc = plugin.getDescription();
        Lang.send(sender, Lang.PLUGIN_RELOADED
                .replace("{name}", desc.getName())
                .replace("{version}", desc.getVersion()));
    }
}
