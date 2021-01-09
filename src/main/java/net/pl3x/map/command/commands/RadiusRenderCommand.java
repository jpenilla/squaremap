package net.pl3x.map.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.CommandManager;
import net.pl3x.map.command.Pl3xMapCommand;
import net.pl3x.map.configuration.Lang;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RadiusRenderCommand extends Pl3xMapCommand {

    public RadiusRenderCommand(final @NonNull Pl3xMap plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        final Command<CommandSender> anySender = this.commandManager.commandBuilder("pl3xmap")
                .argument(WorldArgument.of("world"))
                .literal("radiusrender")
                .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                .argument(StringArgument.optional("center")) // todo: implement Vec2D argument for center
                .meta(CommandMeta.DESCRIPTION, "Starts a radius render")
                .permission("pl3xmap.command.radiusrender")
                .handler(this::anySender)
                .build();

        final Command<CommandSender> player = this.commandManager.commandBuilder("pl3xmap")
                .literal("radiusrender")
                .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                .argument(StringArgument.optional("center")) // todo: implement Vec2D argument for center
                .meta(CommandMeta.DESCRIPTION, "Starts a radius render")
                .permission("pl3xmap.command.radiusrender")
                .senderType(Player.class)
                .handler(this::player)
                .build();

        this.commandManager.commands(anySender, player);
    }

    private void player(final @NonNull CommandContext<CommandSender> context) {
        final Player sender = (Player) context.getSender();
        final World world = sender.getWorld();
        this.executeRadiusRender(sender, world, 0, 0, 0);
    }

    private void anySender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final World world = context.get("world");
        this.executeRadiusRender(sender, world, 0, 0, 0);
    }

    private void executeRadiusRender(final @NonNull CommandSender sender, final @NonNull World world, final int radius, final int centerX, final int centerZ) {
        Lang.send(sender, "Not yet implemented");
    }
}
