package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.command.argument.MapWorldArgument;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import net.pl3x.map.plugin.task.render.RadiusRender;
import net.pl3x.map.plugin.util.CommandUtil;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class RadiusRenderCommand extends Pl3xMapCommand {

    public RadiusRenderCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("radiusrender")
                        .argument(MapWorldArgument.of("world"))
                        .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                        .argument(Location2DArgument.optional("center"), CommandUtil.description(Lang.OPTIONAL_CENTER_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RADIUSRENDER_COMMAND_DESCRIPTION))
                        .permission("pl3xmap.command.radiusrender")
                        .handler(this::executeRadiusRender));
    }

    private void executeRadiusRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = context.get("world");
        final int radius = context.get("radius");

        Location2D center = context.getOrDefault("center", null);
        if (center == null) {
            center = Location2D.from(world.bukkit(), 0, 0);
        }

        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS, Template.template("world", world.name()));
            return;
        }

        Lang.send(sender, Lang.LOG_STARTED_RADIUSRENDER, Template.template("world", world.name()));
        world.startRender(new RadiusRender(center, radius));
    }
}
