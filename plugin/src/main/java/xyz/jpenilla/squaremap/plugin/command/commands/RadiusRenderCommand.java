package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.minecraft.core.BlockPos;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.task.render.RadiusRender;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.plugin.data.PaperMapWorld;
import xyz.jpenilla.squaremap.plugin.util.CommandUtil;

public final class RadiusRenderCommand extends SquaremapCommand {

    public RadiusRenderCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("radiusrender")
                .argument(MapWorldArgument.of("world"))
                .argument(IntegerArgument.<CommandSender>newBuilder("radius").withMin(1).build())
                .argument(Location2DArgument.optional("center"), CommandUtil.description(Lang.OPTIONAL_CENTER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.RADIUSRENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.radiusrender")
                .handler(this::executeRadiusRender));
    }

    private void executeRadiusRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final PaperMapWorld world = context.get("world");
        final int radius = context.get("radius");

        Location2D center = context.getOrDefault("center", null);
        if (center == null) {
            center = Location2D.from(world.bukkit(), 0, 0);
        }

        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS, Template.template("world", world.identifier().asString()));
            return;
        }

        Lang.send(sender, Lang.LOG_STARTED_RADIUSRENDER, Template.template("world", world.identifier().asString()));
        world.startRender(new RadiusRender(world, new BlockPos(center.getBlockX(), 0, center.getBlockZ()), radius));
    }
}
