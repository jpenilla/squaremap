package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import com.google.inject.Inject;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

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
                .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.FULLRENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.fullrender")
                .handler(this::executeFullRender));
    }

    private void executeFullRender(final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);
        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS, Placeholder.unparsed("world", world.identifier().asString()));
            return;
        }

        if (sender instanceof PlayerCommander) {
            Lang.send(sender, Lang.LOG_STARTED_FULLRENDER, Placeholder.unparsed("world", world.identifier().asString()));
        }
        world.startRender(this.renderFactory.createFullRender(world));
    }
}
