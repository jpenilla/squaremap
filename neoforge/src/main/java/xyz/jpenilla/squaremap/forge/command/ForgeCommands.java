package xyz.jpenilla.squaremap.forge.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.brigadier.argument.WrappedBrigadierParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.neoforge.NeoForgeCommandContextKeys;
import cloud.commandframework.neoforge.NeoForgeServerCommandManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
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
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Messages;

@DefaultQualifier(NonNull.class)
@Singleton
public final class ForgeCommands implements PlatformCommands {
    @Inject
    private ForgeCommands() {
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final NeoForgeServerCommandManager<Commander> mgr = new NeoForgeServerCommandManager<>(
            CommandExecutionCoordinator.simpleCoordinator(),
            ForgeCommander::from,
            commander -> ((ForgeCommander) commander).stack()
        );

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public CommandArgument<Commander, ?> columnPosArgument(final String name) {
        return CommandArgument.<Commander, BlockPos>ofType(BlockPos.class, name)
            .withParser(new WrappedBrigadierParser<Commander, Coordinates>(ColumnPosArgument::columnPos).map(ForgeCommands::mapToCoordinates))
            .asOptional()
            .build();
    }

    @Override
    public @Nullable BlockPos extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<BlockPos>getOptional(argName).orElse(null);
    }

    @Override
    public CommandArgument<Commander, ?> singlePlayerSelectorArgument(final String name) {
        return CommandArgument.<Commander, ServerPlayer>ofType(ServerPlayer.class, name)
            .withParser(singlePlayerSelector())
            .build();
    }

    @Override
    public ServerPlayer extractPlayer(final String argName, final CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final @Nullable ServerPlayer specified = context.getOrDefault(argName, null);

        if (specified == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player();
            }
            throw CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER);
        }

        return specified;
    }

    private static <C> ArgumentParseResult<BlockPos> mapToCoordinates(final CommandContext<C> ctx, final Coordinates coordinates) {
        return ArgumentParseResult.success(coordinates.getBlockPos(ctx.get(NeoForgeCommandContextKeys.NATIVE_COMMAND_SOURCE)));
    }

    public static <C> ArgumentParser<C, ServerPlayer> singlePlayerSelector() {
        return new WrappedBrigadierParser<C, EntitySelector>(EntityArgument.player())
            .map((ctx, entitySelector) ->
                handleCommandSyntaxExceptionAsFailure(() ->
                    ArgumentParseResult.success(entitySelector.findSinglePlayer(ctx.get(NeoForgeCommandContextKeys.NATIVE_COMMAND_SOURCE)))));
    }

    @FunctionalInterface
    private interface CommandSyntaxExceptionThrowingParseResultSupplier<O> {
        ArgumentParseResult<O> result() throws CommandSyntaxException;
    }

    private static <O> ArgumentParseResult<O> handleCommandSyntaxExceptionAsFailure(
        final CommandSyntaxExceptionThrowingParseResultSupplier<O> resultSupplier
    ) {
        try {
            return resultSupplier.result();
        } catch (final CommandSyntaxException ex) {
            return ArgumentParseResult.failure(ex);
        }
    }
}
