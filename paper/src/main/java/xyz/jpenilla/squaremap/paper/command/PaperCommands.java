package xyz.jpenilla.squaremap.paper.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.SenderMapper;
import cloud.commandframework.arguments.parser.ParserDescriptor;
import cloud.commandframework.bukkit.data.SinglePlayerSelector;
import cloud.commandframework.bukkit.parser.location.Location2D;
import cloud.commandframework.bukkit.parser.location.Location2DParser;
import cloud.commandframework.bukkit.parser.selector.SinglePlayerSelectorParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.ExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
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
        final PaperCommandManager<Commander> mgr = new PaperCommandManager<>(
            this.plugin,
            ExecutionCoordinator.<Commander>builder()
                .synchronizeExecution(Folia.FOLIA)
                .build(),
            SenderMapper.create(
                PaperCommander::from,
                commander -> ((PaperCommander) commander).sender()
            )
        );

        // Don't check capabilities, the versions of Paper we support always have these.
        mgr.registerBrigadier();
        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public ParserDescriptor<Commander, ?> columnPosParser() {
        return Location2DParser.location2DParser();
    }

    @Override
    public Optional<BlockPos> extractColumnPos(final String argName, final CommandContext<Commander> context) {
        return context.<Location2D>optional(argName)
            .map(loc -> new BlockPos(loc.getBlockX(), 0, loc.getBlockZ()));
    }

    @Override
    public ParserDescriptor<Commander, ?> singlePlayerSelectorParser() {
        return SinglePlayerSelectorParser.singlePlayerSelectorParser();
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

        return Optional.of(CraftBukkitReflection.serverPlayer(selector.single()));
    }
}
