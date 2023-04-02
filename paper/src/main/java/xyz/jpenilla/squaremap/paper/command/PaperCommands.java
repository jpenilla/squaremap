package xyz.jpenilla.squaremap.paper.command;

import cloud.commandframework.Command;
import cloud.commandframework.CommandManager;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.bukkit.parsers.location.Location2D;
import cloud.commandframework.bukkit.parsers.location.Location2DArgument;
import cloud.commandframework.bukkit.parsers.selector.SinglePlayerSelectorArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.CommandExecutionException;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.execution.CommandResult;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.services.State;
import cloud.commandframework.types.tuples.Pair;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
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
import xyz.jpenilla.squaremap.paper.util.CraftBukkitReflection;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
@Singleton
public final class PaperCommands implements PlatformCommands {
    private final JavaPlugin plugin;

    @Inject
    private PaperCommands(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final PaperCommandManager<Commander> mgr;
        try {
            mgr = new PaperCommandManager<>(
                this.plugin,
                ExecutionCoordinator::new,
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

    private static final class ExecutionCoordinator<C> extends CommandExecutionCoordinator<C> {
        private final @Nullable ReentrantLock executionLock;

        ExecutionCoordinator(final CommandTree<C> commandTree) {
            super(commandTree);
            this.executionLock = Folia.FOLIA ? new ReentrantLock() : null;
        }

        @Override
        public CompletableFuture<CommandResult<C>> coordinateExecution(
            final CommandContext<C> commandContext,
            final Queue<String> input
        ) {
            final CompletableFuture<CommandResult<C>> completableFuture = new CompletableFuture<>();
            try {
                final Pair<@Nullable Command<C>, @Nullable Exception> pair =
                    this.getCommandTree().parse(commandContext, input);
                if (pair.getSecond() != null) {
                    completableFuture.completeExceptionally(pair.getSecond());
                } else {
                    final Command<C> command = Objects.requireNonNull(pair.getFirst());
                    if (this.getCommandTree().getCommandManager().postprocessContext(commandContext, command) == State.ACCEPTED) {
                        if (this.executionLock != null) {
                            this.executionLock.lock();
                        }
                        try {
                            command.getCommandExecutionHandler().executeFuture(commandContext).get();
                        } catch (final java.util.concurrent.ExecutionException exception) {
                            Throwable cause = exception.getCause();
                            if (cause instanceof CommandExecutionException) {
                                completableFuture.completeExceptionally(cause);
                            } else {
                                completableFuture.completeExceptionally(new CommandExecutionException(cause, commandContext));
                            }
                        } catch (final CommandExecutionException exception) {
                            completableFuture.completeExceptionally(exception);
                        } catch (final Exception exception) {
                            completableFuture.completeExceptionally(new CommandExecutionException(exception, commandContext));
                        } finally {
                            if (this.executionLock != null) {
                                this.executionLock.unlock();
                            }
                        }
                    }
                    completableFuture.complete(new CommandResult<>(commandContext));
                }
            } catch (final Exception e) {
                completableFuture.completeExceptionally(e);
            }
            return completableFuture;
        }
    }
}
