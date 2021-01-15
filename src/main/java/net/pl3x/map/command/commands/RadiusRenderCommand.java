package net.pl3x.map.command.commands;

import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.CommandManager;
import net.pl3x.map.command.Pl3xMapCommand;
import net.pl3x.map.command.argument.MapWorldArgument;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.data.MapWorld;
import net.pl3x.map.task.RadiusRender;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class RadiusRenderCommand extends Pl3xMapCommand {

    public RadiusRenderCommand(final @NonNull Pl3xMap plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("radiusrender")
                        .argument(MapWorldArgument.of("world"))
                        .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                        .argument(Location2DArgument.optional("center"))
                        .meta(CommandMeta.DESCRIPTION, "Starts a radius render")
                        .permission("pl3xmap.command.radiusrender")
                        .handler(this::executeRadiusRender));
    }

    private void executeRadiusRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = context.get("world");
        final Location2D center = context.getOrDefault("center", Location2D.from(world.bukkit(), 0, 0));
        final int radius = context.get("radius");

        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS
                    .replace("{world}", world.name()));
            return;
        }

        Lang.send(sender, Lang.LOG_STARTED_RADIUSRENDER
                .replace("{world}", world.name()));
        world.startRender(new RadiusRender(center, radius));
    }
}
