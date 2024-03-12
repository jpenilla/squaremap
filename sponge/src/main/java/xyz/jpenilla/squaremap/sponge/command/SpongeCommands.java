package xyz.jpenilla.squaremap.sponge.command;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
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
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.sponge.SpongeCommandManager;
import org.incendo.cloud.sponge.data.SinglePlayerSelector;
import org.incendo.cloud.sponge.parser.SinglePlayerSelectorParser;
import org.incendo.cloud.sponge.parser.Vector2iParser;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeTypes;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.argument.parser.LevelParser;
import xyz.jpenilla.squaremap.common.command.argument.parser.MapWorldParser;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;

@DefaultQualifier(NonNull.class)
@Singleton
public final class SpongeCommands implements PlatformCommands {
    private final PluginContainer pluginContainer;

    @Inject
    private SpongeCommands(final PluginContainer pluginContainer) {
        this.pluginContainer = pluginContainer;
    }

    @Override
    public CommandManager<Commander> createCommandManager() {
        final SpongeCommandManager<Commander> mgr = new SpongeCommandManager<>(
            this.pluginContainer,
            ExecutionCoordinator.simpleCoordinator(),
            SenderMapper.create(
                SpongeCommander::from,
                commander -> ((SpongeCommander) commander).cause()
            )
        );

        mgr.parserMapper().cloudNumberSuggestions(true);
        mgr.parserMapper().registerMapping(new TypeToken<LevelParser<Commander>>() {}, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });
        mgr.parserMapper().registerMapping(new TypeToken<MapWorldParser<Commander>>() {}, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });

        return mgr;
    }

    @Override
    public ParserDescriptor<Commander, ?> columnPosParser() {
        return Vector2iParser.vector2iParser();
    }

    @Override
    public Optional<BlockPos> extractColumnPos(final String name, final CommandContext<Commander> ctx) {
        return ctx.<Vector2i>optional(name)
            .map(loc -> new BlockPos(loc.x(), 0, loc.y()));
    }

    @Override
    public ParserDescriptor<Commander, ?> singlePlayerSelectorParser() {
        return SinglePlayerSelectorParser.singlePlayerSelectorParser();
    }

    @Override
    public Optional<ServerPlayer> extractPlayer(final String name, final CommandContext<Commander> ctx) throws CommandCompleted {
        final Commander sender = ctx.sender();
        final @Nullable SinglePlayerSelector selector = ctx.getOrDefault(name, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return Optional.of(player.player());
            }
            return Optional.empty();
        }

        return Optional.of((ServerPlayer) selector.getSingle());
    }
}
