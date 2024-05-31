package xyz.jpenilla.squaremap.paper.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.bukkit.data.SinglePlayerSelector;
import org.incendo.cloud.bukkit.parser.location.Location2DParser;
import org.incendo.cloud.bukkit.parser.selector.SinglePlayerSelectorParser;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.ParserDescriptor;
import xyz.jpenilla.squaremap.common.command.BrigadierSetup;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.paper.util.CraftBukkitHelper;
import xyz.jpenilla.squaremap.paper.util.Folia;

@DefaultQualifier(NonNull.class)
@Singleton
@SuppressWarnings("UnstableApiUsage")
public final class PaperCommands implements PlatformCommands {
    private final JavaPlugin plugin;

    @Inject
    private PaperCommands(final JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final SenderMapper<CommandSourceStack, Commander> senderMapper = SenderMapper.create(
            PaperCommander::from,
            commander -> ((PaperCommander) commander).stack()
        );

        final PaperCommandManager<Commander> mgr = PaperCommandManager.builder(senderMapper)
            .executionCoordinator(ExecutionCoordinator.<Commander>builder()
                .synchronizeExecution(Folia.FOLIA)
                .build())
            .buildOnEnable(this.plugin);

        BrigadierSetup.setup(mgr);

        return mgr;
    }

    @Override
    public ParserDescriptor<Commander, BlockPos> columnPosParser() {
        return Location2DParser.<Commander>location2DParser().mapSuccess(
            BlockPos.class,
            (ctx, loc) -> CompletableFuture.completedFuture(new BlockPos(loc.getBlockX(), 0, loc.getBlockZ()))
        );
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

        return Optional.of(CraftBukkitHelper.serverPlayer(selector.single()));
    }
}
