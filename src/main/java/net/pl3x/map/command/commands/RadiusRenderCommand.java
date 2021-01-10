package net.pl3x.map.command.commands;

import cloud.commandframework.Command;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.WorldArgument;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.CommandManager;
import net.pl3x.map.command.Pl3xMapCommand;
import net.pl3x.map.configuration.Lang;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class RadiusRenderCommand extends Pl3xMapCommand {

    public RadiusRenderCommand(final @NonNull Pl3xMap plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        final Command<CommandSender> radiusRender = this.commandManager.commandBuilder("pl3xmap")
                .literal("radiusrender")
                .argument(WorldArgument.of("world"))
                .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                .argument(Location2DArgument.optional("center"))
                .meta(CommandMeta.DESCRIPTION, "Starts a radius render")
                .permission("pl3xmap.command.radiusrender")
                .handler(this::executeRadiusRender)
                .build();
        this.commandManager.command(radiusRender);
    }

    private void executeRadiusRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final World world = context.get("world");
        final Location2D center = context.getOrDefault("center", Location2D.from(world, 0, 0));
        final double centerX = center.getX();
        final double centerZ = center.getZ();
        Lang.send(sender, "Not yet implemented");
    }
}
