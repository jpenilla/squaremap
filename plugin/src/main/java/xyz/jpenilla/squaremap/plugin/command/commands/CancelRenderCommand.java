package xyz.jpenilla.squaremap.plugin.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.Commands;
import xyz.jpenilla.squaremap.plugin.command.SquaremapCommand;
import xyz.jpenilla.squaremap.plugin.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.plugin.configuration.Lang;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.util.CommandUtil;

public final class CancelRenderCommand extends SquaremapCommand {

    public CancelRenderCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("cancelrender")
                        .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.CANCEL_RENDER_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.cancelrender")
                        .handler(this::executeCancelRender));
    }

    private void executeCancelRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = CommandUtil.resolveWorld(context);
        if (!world.isRendering()) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS, Template.template("world", world.name()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER, Template.template("world", world.name()));
        world.stopRender();
        world.finishedRender();
    }

}
