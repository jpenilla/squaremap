package xyz.jpenilla.squaremap.common.command.commands;

import com.google.inject.Inject;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.Components;

import static org.incendo.cloud.minecraft.extras.RichDescription.richDescription;
import static org.incendo.cloud.parser.standard.IntegerParser.integerParser;
import static xyz.jpenilla.squaremap.common.command.argument.parser.MapWorldParser.mapWorldParser;

@DefaultQualifier(NonNull.class)
public final class RadiusRenderCommand extends SquaremapCommand {
    private final PlatformCommands platformCommands;

    @Inject
    private RadiusRenderCommand(
        final Commands commands,
        final PlatformCommands platformCommands
    ) {
        super(commands);
        this.platformCommands = platformCommands;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("radiusrender")
                .required("world", mapWorldParser())
                .required("radius", integerParser(1))
                .optional("center", this.platformCommands.columnPosParser(), richDescription(Messages.OPTIONAL_CENTER_ARGUMENT_DESCRIPTION))
                .commandDescription(richDescription(Messages.RADIUSRENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.radiusrender")
                .handler(this::executeRadiusRender));
    }

    private void executeRadiusRender(final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final MapWorldInternal world = context.get("world");
        final int radius = context.get("radius");

        @Nullable BlockPos center = this.platformCommands.extractColumnPos("center", context).orElse(null);
        if (center == null) {
            center = new BlockPos(0, 0, 0);
        }

        if (world.renderManager().isRendering()) {
            sender.sendMessage(Messages.RENDER_IN_PROGRESS.withPlaceholders(Components.worldPlaceholder(world)));
            return;
        }

        sender.sendMessage(Components.miniMessage(Messages.LOG_STARTED_RADIUSRENDER, Components.worldPlaceholder(world)));
        world.renderManager().startRender(
            context.get(Commands.RENDER_FACTORY).createRadiusRender(world, center, radius)
        );
    }
}
