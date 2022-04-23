package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import cloud.commandframework.minecraft.extras.RichDescription;
import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

@DefaultQualifier(NonNull.class)
public final class PauseRenderCommand extends SquaremapCommand {
    @Inject
    private PauseRenderCommand(final Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("pauserender")
                .argument(MapWorldArgument.optional("world"), RichDescription.of(Messages.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Messages.PAUSE_RENDER_COMMAND_DESCRIPTION.asComponent())
                .permission("squaremap.command.pauserender")
                .handler(this::executePauseRender));
    }

    private void executePauseRender(final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);

        world.pauseRenders(!world.rendersPaused());

        if (world.rendersPaused()) {
            sender.sendMessage(Messages.PAUSED_RENDER.withPlaceholders(Components.worldPlaceholder(world)));
        } else {
            sender.sendMessage(Messages.UNPAUSED_RENDER.withPlaceholders(Components.worldPlaceholder(world)));
        }
    }
}
