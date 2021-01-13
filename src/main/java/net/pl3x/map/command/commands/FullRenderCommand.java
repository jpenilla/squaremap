package net.pl3x.map.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.WorldManager;
import net.pl3x.map.command.CommandManager;
import net.pl3x.map.command.Pl3xMapCommand;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.task.FullRender;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class FullRenderCommand extends Pl3xMapCommand {

    public FullRenderCommand(final @NonNull Pl3xMap plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        final Command<CommandSender> anySender = this.commandManager.commandBuilder("pl3xmap")
                .literal("fullrender")
                .argument(WorldArgument.of("world"))
                .meta(CommandMeta.DESCRIPTION, "Starts a full render for the specified world")
                .permission("pl3xmap.command.fullrender")
                .handler(this::anySender)
                .build();

        final Command<CommandSender> player = this.commandManager.commandBuilder("pl3xmap")
                .literal("fullrender")
                .meta(CommandMeta.DESCRIPTION, "Starts a full render for the world you are currently in")
                .meta(CommandManager.INVALID_SENDER_ALTERNATE_COMMAND, "pl3xmap fullrender <world>")
                .permission("pl3xmap.command.fullrender")
                .senderType(Player.class)
                .handler(this::player)
                .build();

        this.commandManager.commands(anySender, player);
    }

    private void player(final @NonNull CommandContext<CommandSender> context) {
        final Player sender = (Player) context.getSender();
        final World world = sender.getWorld();
        this.executeFullRender(sender, world);
    }

    private void anySender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final World world = context.get("world");
        this.executeFullRender(sender, world);
    }

    private void executeFullRender(final @NonNull CommandSender sender, final @NonNull World world) {
        if (WorldManager.getWorld(world).isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS
                    .replace("{world}", world.getName()));
            return;
        }

        Lang.send(sender, Lang.FULL_RENDER_STARTED
                .replace("{world}", world.getName()));
        WorldManager.getWorld(world).startRender(new FullRender(world));
    }
}
