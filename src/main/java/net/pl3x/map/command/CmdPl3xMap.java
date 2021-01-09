package net.pl3x.map.command;

import cloud.commandframework.annotations.Argument;
import cloud.commandframework.annotations.CommandDescription;
import cloud.commandframework.annotations.CommandMethod;
import cloud.commandframework.annotations.CommandPermission;
import cloud.commandframework.annotations.Flag;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public class CmdPl3xMap {
    private final Pl3xMap plugin;

    public CmdPl3xMap(Pl3xMap plugin) {
        this.plugin = plugin;
    }

    @CommandDescription("Starts a full render")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap|map fullrender [world]")
    private void onFullRender(
            CommandSender sender,
            @Argument("world") @Nullable World world
    ) {
        world = this.resolveWorld(sender, world);
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
    @CommandMethod("pl3xmap radiusrender <radius> [world]")
    private void onRadiusRender(
            CommandSender sender,
            @Argument("radius") int radius,
            @Argument("world") @Nullable World world,
            @Flag(value = "center", aliases = {"c"}, description = "Center position") @Nullable String center
    ) {
        world = resolveWorld(sender, world);
        // todo: implement Vec2D argument for center
        Lang.send(sender, "Not implemented yet");
    }

    @CommandDescription("Cancels a render")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap cancelrender [world]")
    private void onCancelRender(
            CommandSender sender,
            @Argument("world") @Nullable World world
    ) {
        world = this.resolveWorld(sender, world);
        if (!RenderManager.isRendering(world)) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS
                    .replace("{world}", world.getName()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER
                .replace("{world}", world.getName()));
        RenderManager.cancelRender(world);
    }

    private @NonNull World resolveWorld(final @NonNull CommandSender sender, final @Nullable World world) {
        if (world != null) {
            return world;
        }
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        throw new ConsoleMustProvidePlayerException();
    }

    @CommandDescription("Reloads the plugin")
    @CommandPermission("command.pl3xmap")
    @CommandMethod("pl3xmap reload")
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
