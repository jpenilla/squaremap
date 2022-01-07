package xyz.jpenilla.squaremap.fabric.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricServerCommandManager;
import cloud.commandframework.fabric.argument.server.ColumnPosArgument;
import cloud.commandframework.fabric.argument.server.SinglePlayerSelectorArgument;
import cloud.commandframework.fabric.data.Coordinates;
import cloud.commandframework.fabric.data.SinglePlayerSelector;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import xyz.jpenilla.squaremap.common.SquaremapCommon;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.commands.HideShowCommands;
import xyz.jpenilla.squaremap.common.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;

public final class FabricCommands {
    private FabricCommands() {
    }

    public static CommandManager<Commander> createCommandManager() {
        final FabricServerCommandManager<Commander> mgr = new FabricServerCommandManager<>(
            CommandExecutionCoordinator.simpleCoordinator(),
            FabricCommander::from,
            commander -> ((FabricCommander) commander).stack()
        );

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    public static void register(final @NonNull SquaremapCommon common) {
        List.of(
            new RadiusRenderCommand(
                common.commands(),
                ColumnPosArgument::optional,
                (name, context) -> {
                    final Coordinates.@Nullable ColumnCoordinates loc = context.getOrDefault(name, null);
                    if (loc == null) {
                        return null;
                    }
                    return loc.blockPos();
                }
            ),
            new HideShowCommands(common.commands(), SinglePlayerSelectorArgument::of, FabricCommands::resolvePlayer)
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

        return selector.getSingle();
    }
}
