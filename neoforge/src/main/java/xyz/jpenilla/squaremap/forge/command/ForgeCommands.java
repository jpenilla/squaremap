package xyz.jpenilla.squaremap.forge.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.SenderMapper;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.arguments.parser.ParserDescriptor;
import cloud.commandframework.brigadier.argument.WrappedBrigadierParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.ExecutionCoordinator;
import cloud.commandframework.neoforge.NeoForgeCommandContextKeys;
import cloud.commandframework.neoforge.NeoForgeServerCommandManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.ColumnPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgeCommands implements PlatformCommands {
    @Inject
    private ForgeCommands() {
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final NeoForgeServerCommandManager<Commander> mgr = new NeoForgeServerCommandManager<>(
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create(
                ForgeCommander::from,
                commander -> ((ForgeCommander) commander).stack()
            )
        );

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public ParserDescriptor<Commander, ?> columnPosParser() {
        return ParserDescriptor.of(
            new WrappedBrigadierParser<Commander, Coordinates>(ColumnPosArgument::columnPos)
                .flatMapSuccess(ForgeCommands::mapToCoordinates),
            BlockPos.class
        );
    }

    @Override
    public Optional<BlockPos> extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<BlockPos>optional(argName);
    }

    @Override
    public ParserDescriptor<Commander, ?> singlePlayerSelectorParser() {
        return ParserDescriptor.of(singlePlayerSelector(), ServerPlayer.class);
    }

    @Override
    public Optional<ServerPlayer> extractPlayer(final String argName, final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final @Nullable ServerPlayer specified = context.getOrDefault(argName, null);

        if (specified == null) {
            if (sender instanceof PlayerCommander player) {
                return Optional.of(player.player());
            }
            return Optional.empty();
        }

        return Optional.of(specified);
    }

    private static <C> CompletableFuture<ArgumentParseResult<BlockPos>> mapToCoordinates(final CommandContext<C> ctx, final Coordinates coordinates) {
        return ArgumentParseResult.successFuture(coordinates.getBlockPos(ctx.get(NeoForgeCommandContextKeys.NATIVE_COMMAND_SOURCE)));
    }

    public static <C> ArgumentParser<C, ServerPlayer> singlePlayerSelector() {
        return new WrappedBrigadierParser<C, EntitySelector>(EntityArgument.player())
            .flatMapSuccess((ctx, entitySelector) ->
                handleCommandSyntaxExceptionAsFailure(() ->
                    ArgumentParseResult.success(entitySelector.findSinglePlayer(ctx.get(NeoForgeCommandContextKeys.NATIVE_COMMAND_SOURCE)))));
    }

    @FunctionalInterface
    private interface CommandSyntaxExceptionThrowingParseResultSupplier<O> {
        ArgumentParseResult<O> result() throws CommandSyntaxException;
    }

    private static <O> CompletableFuture<ArgumentParseResult<O>> handleCommandSyntaxExceptionAsFailure(
        final CommandSyntaxExceptionThrowingParseResultSupplier<O> resultSupplier
    ) {
        try {
            return CompletableFuture.completedFuture(resultSupplier.result());
        } catch (final CommandSyntaxException ex) {
            return ArgumentParseResult.failureFuture(ex);
        }
    }
}
