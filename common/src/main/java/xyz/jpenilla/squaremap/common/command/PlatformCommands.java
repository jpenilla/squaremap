package xyz.jpenilla.squaremap.common.command;

import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.ParserDescriptor;
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
     * Create a column pos parser.
     *
     * @return built argument
     */
    ParserDescriptor<Commander, BlockPos> columnPosParser();

    /**
     * Create a single player selector parser.
     *
     * @return built argument
     */
    ParserDescriptor<Commander, ?> singlePlayerSelectorParser();

    /**
     * Extract the result from an argument created with {@link #singlePlayerSelectorParser()}.
     *
     * @param name argument name
     * @param ctx  command context
     * @return result
     * @throws CommandCompleted when no result is found
     */
    Optional<ServerPlayer> extractPlayer(String name, CommandContext<Commander> ctx);
}
