package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

public final class CancelRenderCommand extends SquaremapCommand {
    public CancelRenderCommand(final @NonNull Commands commands) {
        super(commands);
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("cancelrender")
                .argument(MapWorldArgument.optional("world"), CommandUtil.description(Lang.OPTIONAL_WORLD_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.CANCEL_RENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.cancelrender")
                .handler(this::executeCancelRender));
    }

    private void executeCancelRender(final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = CommandUtil.resolveWorld(context);
        if (!world.isRendering()) {
            Lang.send(sender, Lang.RENDER_NOT_IN_PROGRESS, Placeholder.unparsed("world", world.identifier().asString()));
            return;
        }

        Lang.send(sender, Lang.CANCELLED_RENDER, Placeholder.unparsed("world", world.identifier().asString()));
        world.stopRender();
        world.finishedRender();
    }
}
