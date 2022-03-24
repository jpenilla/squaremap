package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.config.Lang;
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
                .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.PAUSE_RENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.pauserender")
                .handler(this::executePauseRender));
    }

    private void executePauseRender(final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);

        world.pauseRenders(!world.rendersPaused());

        if (world.rendersPaused()) {
            Lang.send(sender, Lang.PAUSED_RENDER, Placeholder.unparsed("world", world.identifier().asString()));
        } else {
            Lang.send(sender, Lang.UNPAUSED_RENDER, Placeholder.unparsed("world", world.identifier().asString()));
        }
    }
}
