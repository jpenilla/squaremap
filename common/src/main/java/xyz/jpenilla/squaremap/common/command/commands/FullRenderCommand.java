package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;
import static xyz.jpenilla.squaremap.common.command.argument.parser.MapWorldParser.mapWorldParser;

@DefaultQualifier(NonNull.class)
public final class FullRenderCommand extends SquaremapCommand {
    private final RenderFactory renderFactory;

    @Inject
    private FullRenderCommand(
        final Commands commands,
        final RenderFactory renderFactory
    ) {
        super(commands);
        this.renderFactory = renderFactory;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("fullrender")
                .optional("world", mapWorldParser(), richDescription(Messages.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .commandDescription(richDescription(Messages.FULLRENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.fullrender")
                .handler(this::executeFullRender));
    }

    private void executeFullRender(final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);
        if (world.renderManager().isRendering()) {
            sender.sendMessage(Messages.RENDER_IN_PROGRESS.withPlaceholders(Components.worldPlaceholder(world)));
            return;
        }

        if (sender instanceof PlayerCommander) {
            sender.sendMessage(Components.miniMessage(Messages.LOG_STARTED_FULLRENDER, Components.worldPlaceholder(world)));
        }
        world.renderManager().startRender(this.renderFactory.createFullRender(world));
    }
}
