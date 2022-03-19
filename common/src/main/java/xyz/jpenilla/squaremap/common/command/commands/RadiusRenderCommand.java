package xyz.jpenilla.squaremap.common.command.commands;

import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;
import xyz.jpenilla.squaremap.common.task.render.RenderFactory;
import xyz.jpenilla.squaremap.common.util.CommandUtil;
import xyz.jpenilla.squaremap.common.util.Components;

public final class RadiusRenderCommand extends SquaremapCommand {
    private final Function<String, CommandArgument<Commander, ?>> vec2dArgument;
    private final BiFunction<String, CommandContext<Commander>, @Nullable BlockPos> getPos;

    public RadiusRenderCommand(
        final @NonNull Commands commands,
        final @NonNull Function<String, CommandArgument<Commander, ?>> vec2dArgument,
        final @NonNull BiFunction<String, CommandContext<Commander>, @Nullable BlockPos> getPos
    ) {
        super(commands);
        this.vec2dArgument = vec2dArgument;
        this.getPos = getPos;
    }

    @Override
    public void register() {
        this.commands.registerSubcommand(builder ->
            builder.literal("radiusrender")
                .argument(MapWorldArgument.of("world"))
                .argument(IntegerArgument.<Commander>newBuilder("radius").withMin(1).build())
                .argument(this.vec2dArgument.apply("center"), CommandUtil.description(Lang.OPTIONAL_CENTER_ARGUMENT_DESCRIPTION))
                .meta(MinecraftExtrasMetaKeys.DESCRIPTION, Components.miniMessage(Lang.RADIUSRENDER_COMMAND_DESCRIPTION))
                .permission("squaremap.command.radiusrender")
                .handler(this::executeRadiusRender));
    }

    private void executeRadiusRender(final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = context.get("world");
        final int radius = context.get("radius");

        BlockPos center = this.getPos.apply("center", context);
        if (center == null) {
            center = new BlockPos(0, 0, 0);
        }

        if (world.isRendering()) {
            Lang.send(sender, Lang.RENDER_IN_PROGRESS, Placeholder.unparsed("world", world.identifier().asString()));
            return;
        }

        Lang.send(sender, Lang.LOG_STARTED_RADIUSRENDER, Placeholder.unparsed("world", world.identifier().asString()));
        world.startRender(
            context.get(Commands.INJECTOR).getInstance(RenderFactory.class)
                .createRadiusRender(world, center, radius)
        );
    }
}
