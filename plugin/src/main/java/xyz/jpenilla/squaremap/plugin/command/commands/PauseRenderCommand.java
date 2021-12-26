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
import xyz.jpenilla.squaremap.plugin.config.Lang;
import xyz.jpenilla.squaremap.plugin.data.MapWorld;
import xyz.jpenilla.squaremap.plugin.util.CommandUtil;

public final class PauseRenderCommand extends SquaremapCommand {

    public PauseRenderCommand(final @NonNull SquaremapPlugin plugin, final @NonNull Commands commands) {
        super(plugin, commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
                builder.literal("pauserender")
                        .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().parse(Lang.PAUSE_RENDER_COMMAND_DESCRIPTION))
                        .permission("squaremap.command.pauserender")
                        .handler(this::executePauseRender));
    }

    private void executePauseRender(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = CommandUtil.resolveWorld(context);

        world.pauseRenders(!world.rendersPaused());

        if (world.rendersPaused()) {
            Lang.send(sender, Lang.PAUSED_RENDER, Template.template("world", world.name()));
        } else {
            Lang.send(sender, Lang.UNPAUSED_RENDER, Template.template("world", world.name()));
        }
    }

}
