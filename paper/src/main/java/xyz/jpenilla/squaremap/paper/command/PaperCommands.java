package xyz.jpenilla.squaremap.paper.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.Components;
import xyz.jpenilla.squaremap.paper.SquaremapPaper;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperCommands implements PlatformCommands {
    private final SquaremapPaper plugin;

    @Inject
    private PaperCommands(final SquaremapPaper plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final PaperCommandManager<Commander> mgr;
        try {
            mgr = new PaperCommandManager<>(
                this.plugin,
                CommandExecutionCoordinator.simpleCoordinator(),
                PaperCommander::from,
                commander -> ((PaperCommander) commander).sender()
            );
        } catch (final Exception ex) {
            throw new RuntimeException("Failed to initialize command manager", ex);
        }

        // Don't check capabilities, the versions of Paper we support always have these.
        mgr.registerBrigadier();
        BrigadierSetup.setup(mgr);
        mgr.registerAsynchronousCompletions();

        return mgr;
    }

    @Override
    public CommandArgument<Commander, ?> columnPosArgument(final String name) {
        return Location2DArgument.optional(name);
    }

    @Override
    public @Nullable BlockPos extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<Location2D>getOptional(argName)
            .map(loc -> new BlockPos(loc.getBlockX(), 0, loc.getBlockZ()))
            .orElse(null);
    }

    @Override
    public CommandArgument<Commander, ?> singlePlayerSelectorArgument(final String name) {
        return SinglePlayerSelectorArgument.of(name);
    }

    @Override
    public ServerPlayer extractPlayer(final String argName, final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final @Nullable SinglePlayerSelector selector = context.getOrDefault(argName, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player();
            }
            throw CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER);
        }

        final @Nullable Player targetPlayer = selector.getPlayer();
        if (targetPlayer == null) {
            throw CommandCompleted.withMessage(Messages.PLAYER_NOT_FOUND_FOR_INPUT.withPlaceholders(Components.placeholder("input", selector.getSelector())));
        }

        return CraftBukkitReflection.serverPlayer(targetPlayer);
    }
}
