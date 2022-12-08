package xyz.jpenilla.squaremap.common.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;

@DefaultQualifier(NonNull.class)
public interface PlatformCommands {
    /**
     * Create the platform command manager.
     * 
     * @return command manager
     */
    CommandManager<Commander> createCommandManager();

    /**
     * Create an optional column pos argument.
     *
     * @param name argument name
     * @return built argument
     */
    CommandArgument<Commander, ?> columnPosArgument(String name);

    /**
     * Extract the optional result from an argument created with {@link #columnPosArgument(String)}.
     *
     * @param name argument name
     * @param ctx  command context
     * @return result or null
     */
    @Nullable BlockPos extractColumnPos(String name, CommandContext<Commander> ctx);

    /**
     * Create a required single player selector argument.
     *
     * @param name argument name
     * @return built argument
     */
    CommandArgument<Commander, ?> singlePlayerSelectorArgument(String name);

    /**
     * Extract the result from an argument created with {@link #singlePlayerSelectorArgument(String)}.
     *
     * @param name argument name
     * @param ctx  command context
     * @return result
     * @throws CommandCompleted when no result is found
     */
    ServerPlayer extractPlayer(String name, CommandContext<Commander> ctx) throws CommandCompleted;
}
