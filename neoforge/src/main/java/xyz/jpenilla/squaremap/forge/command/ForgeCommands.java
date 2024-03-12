package xyz.jpenilla.squaremap.forge.command;

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
import org.incendo.cloud.minecraft.modded.data.Coordinates;
import org.incendo.cloud.minecraft.modded.parser.VanillaArgumentParsers;
import org.incendo.cloud.neoforge.NeoForgeServerCommandManager;
import org.incendo.cloud.parser.ParserDescriptor;
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
        return VanillaArgumentParsers.columnPosParser();
    }

    @Override
    public Optional<BlockPos> extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<org.incendo.cloud.minecraft.modded.data.Coordinates.ColumnCoordinates>optional(argName)
            .map(Coordinates::blockPos);
    }

    @Override
    public ParserDescriptor<Commander, ?> singlePlayerSelectorParser() {
        return VanillaArgumentParsers.singlePlayerSelectorParser();
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
}
