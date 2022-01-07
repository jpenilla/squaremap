package xyz.jpenilla.squaremap.sponge.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.sponge.SpongeCommandManager;
import cloud.commandframework.sponge.argument.SinglePlayerSelectorArgument;
import cloud.commandframework.sponge.argument.Vector2iArgument;
import cloud.commandframework.sponge.data.SinglePlayerSelector;
import io.leangen.geantyref.TypeToken;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeTypes;
import org.spongepowered.math.vector.Vector2i;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.argument.LevelArgument;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.command.commands.HideShowCommands;
import xyz.jpenilla.squaremap.common.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.sponge.SquaremapSponge;

public final class SpongeCommands {
    private SpongeCommands() {
    }

    public static CommandManager<Commander> createCommandManager(final SquaremapSponge plugin) {
        final SpongeCommandManager<Commander> mgr = new SpongeCommandManager<>(
            plugin.pluginContainer(),
            CommandExecutionCoordinator.simpleCoordinator(),
            commander -> ((SpongeCommander) commander).cause(),
            SpongeCommander::from
        );

        mgr.parserMapper().cloudNumberSuggestions(true);
        mgr.parserMapper().registerMapping(new TypeToken<LevelArgument.Parser<Commander>>() {
        }, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });
        mgr.parserMapper().registerMapping(new TypeToken<MapWorldArgument.Parser<Commander>>() {
        }, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });

        return mgr;
    }

    public static void register(final @NonNull SquaremapCommon common) {
        List.of(
            new RadiusRenderCommand(
                common.commands(),
                Vector2iArgument::optional,
                (name, context) -> {
                    final @Nullable Vector2i loc = context.getOrDefault(name, null);
                    if (loc == null) {
                        return null;
                    }
                    return new BlockPos(loc.x(), 0, loc.y());
                }
            ),
            new HideShowCommands(common.commands(), SinglePlayerSelectorArgument::of, SpongeCommands::resolvePlayer)
        ).forEach(SquaremapCommand::register);
    }

    private static @NonNull ServerPlayer resolvePlayer(final @NonNull String argName, final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final SinglePlayerSelector selector = context.getOrDefault(argName, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player();
            }
            throw CommandCompleted.withMessage(Components.miniMessage(Lang.CONSOLE_MUST_SPECIFY_PLAYER));
        }

        return (ServerPlayer) selector.getSingle();
    }
}
