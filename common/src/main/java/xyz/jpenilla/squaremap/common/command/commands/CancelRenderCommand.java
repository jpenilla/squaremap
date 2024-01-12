package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

import static cloud.commandframework.minecraft.extras.RichDescription.richDescription;
import static xyz.jpenilla.squaremap.common.command.argument.parser.MapWorldParser.mapWorldParser;

@DefaultQualifier(NonNull.class)
public final class CancelRenderCommand extends SquaremapCommand {
    @Inject
    private CancelRenderCommand(final Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("cancelrender")
                .optional("world", mapWorldParser(), richDescription(Messages.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .commandDescription(richDescription(Messages.CANCEL_RENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.cancelrender")
                .handler(this::executeCancelRender));
    }

    private void executeCancelRender(final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);
        if (!world.renderManager().isRendering()) {
            sender.sendMessage(Messages.RENDER_NOT_IN_PROGRESS.withPlaceholders(Components.worldPlaceholder(world)));
            return;
        }

        sender.sendMessage(Messages.CANCELLED_RENDER.withPlaceholders(Components.worldPlaceholder(world)));
        world.renderManager().cancelRender();
    }
}
