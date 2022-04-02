package xyz.jpenilla.squaremap.fabric.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.fabric.FabricServerCommandManager;
import cloud.commandframework.fabric.argument.server.ColumnPosArgument;
import cloud.commandframework.fabric.argument.server.SinglePlayerSelectorArgument;
import cloud.commandframework.fabric.data.Coordinates;
import cloud.commandframework.fabric.data.SinglePlayerSelector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.SquaremapCommand;
import xyz.jpenilla.squaremap.common.command.commands.HideShowCommands;
import xyz.jpenilla.squaremap.common.command.commands.RadiusRenderCommand;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.util.Components;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricCommands implements PlatformCommands {
    @Inject
    private FabricCommands() {
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final FabricServerCommandManager<Commander> mgr = new FabricServerCommandManager<>(
            CommandExecutionCoordinator.simpleCoordinator(),
            FabricCommander::from,
            commander -> ((FabricCommander) commander).stack()
        );

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public void registerCommands(final Commands commands) {
        List.of(
            new RadiusRenderCommand(commands, ColumnPosArgument::optional, FabricCommands::resolveColumnPos),
            new HideShowCommands(commands, SinglePlayerSelectorArgument::of, FabricCommands::resolvePlayer)
        ).forEach(SquaremapCommand::register);
    }

    private static @Nullable BlockPos resolveColumnPos(final String argName, final CommandContext<Commander> context) {
        final Coordinates.@Nullable ColumnCoordinates loc = context.getOrDefault(argName, null);
        if (loc == null) {
            return null;
        }
        return loc.blockPos();
    }

    private static ServerPlayer resolvePlayer(final String argName, final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final @Nullable SinglePlayerSelector selector = context.getOrDefault(argName, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player();
            }
            throw CommandCompleted.withMessage(Components.miniMessage(Lang.CONSOLE_MUST_SPECIFY_PLAYER));
        }

        return selector.getSingle();
    }
}
