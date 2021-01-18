package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.command.argument.MapWorldArgument;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public final class CancelRenderCommand extends Pl3xMapCommand {

    public CancelRenderCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("cancelrender")
                        .argument(MapWorldArgument.of("world"))
                        .meta(CommandMeta.DESCRIPTION, "Cancels a render for the specified world")
                        .permission("pl3xmap.command.cancelrender")
                        .handler(this::anySender));

        this.commandManager.registerSubcommand(builder ->
                builder.literal("cancelrender")
                        .meta(CommandMeta.DESCRIPTION, "Cancels a render for the world you are currently in")
                        .meta(CommandManager.INVALID_SENDER_ALTERNATE_COMMAND, "pl3xmap cancelrender <world>")
                        .permission("pl3xmap.command.cancelrender")
                        .senderType(Player.class)
                        .handler(this::player));
    }

    private void player(final @NonNull CommandContext<CommandSender> context) {
        final Player sender = (Player) context.getSender();
        final World world = sender.getWorld();
        Optional<MapWorld> optionalMapWorld = this.plugin.worldManager().getWorldIfEnabled(world);
        if (optionalMapWorld.isEmpty()) {
            Lang.send(sender, Lang.MAP_NOT_ENABLED_FOR_WORLD.replace("{world}", world.getName()));
            return;
        }
        this.executeCancelRender(sender, optionalMapWorld.get());
    }

    private void anySender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = context.get("world");
        this.executeCancelRender(sender, world);
    }

    private void executeCancelRender(final @NonNull CommandSender sender, final @NonNull MapWorld world) {
        if (!world.isRendering()) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS
                    .replace("{world}", world.name()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER
                .replace("{world}", world.name()));
        world.stopRender();
    }
}
