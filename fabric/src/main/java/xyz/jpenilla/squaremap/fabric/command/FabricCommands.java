package xyz.jpenilla.squaremap.fabric.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;
import org.incendo.cloud.minecraft.modded.data.Coordinates;
import org.incendo.cloud.minecraft.modded.data.SinglePlayerSelector;
import org.incendo.cloud.minecraft.modded.parser.VanillaArgumentParsers;
import org.incendo.cloud.parser.ParserDescriptor;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;

@DefaultQualifier(NonNull.class)
@Singleton
public final class FabricCommands implements PlatformCommands {
    @Inject
    private FabricCommands() {
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final FabricServerCommandManager<Commander> mgr = new FabricServerCommandManager<>(
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create(
                FabricCommander::from,
                commander -> ((FabricCommander) commander).stack()
            )
        );

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public ParserDescriptor<Commander, ?> columnPosParser() {
        return VanillaArgumentParsers.columnPosParser();
    }

    @Override
    public Optional<BlockPos> extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<Coordinates.ColumnCoordinates>optional(argName)
            .map(Coordinates::blockPos);
    }

    @Override
    public ParserDescriptor<Commander, ?> singlePlayerSelectorParser() {
        return VanillaArgumentParsers.singlePlayerSelectorParser();
    }

    @Override
    public Optional<ServerPlayer> extractPlayer(final String argName, final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final @Nullable SinglePlayerSelector selector = context.getOrDefault(argName, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return Optional.of(player.player());
            }
            return Optional.empty();
        }

        return Optional.of(selector.single());
    }
}
