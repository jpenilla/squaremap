package xyz.jpenilla.squaremap.sponge.command;

import cloud.commandframework.CommandManager;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.sponge.SpongeCommandManager;
import cloud.commandframework.sponge.argument.SinglePlayerSelectorArgument;
import cloud.commandframework.sponge.argument.Vector2iArgument;
import cloud.commandframework.sponge.data.SinglePlayerSelector;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.geantyref.TypeToken;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.spongepowered.api.command.registrar.tree.CommandTreeNodeTypes;
import org.spongepowered.math.vector.Vector2i;
import org.spongepowered.plugin.PluginContainer;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.PlatformCommands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.argument.LevelArgument;
import xyz.jpenilla.squaremap.common.command.argument.MapWorldArgument;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Messages;

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
            CommandExecutionCoordinator.simpleCoordinator(),
            commander -> ((SpongeCommander) commander).cause(),
            SpongeCommander::from
        );

        mgr.parserMapper().cloudNumberSuggestions(true);
        mgr.parserMapper().registerMapping(new TypeToken<LevelArgument.Parser<Commander>>() {
        }, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });
        mgr.parserMapper().registerMapping(new TypeToken<MapWorldArgument.Parser<Commander>>() {
        }, builder -> {
            builder.cloudSuggestions(true);
            builder.to(doubleParser -> CommandTreeNodeTypes.DIMENSION.get().createNode());
        });

        return mgr;
    }

    @Override
    public CommandArgument<Commander, ?> columnPosArgument(final String name) {
        return Vector2iArgument.optional(name);
    }

    @Override
    public @Nullable BlockPos extractColumnPos(final String name, final CommandContext<Commander> ctx) {
        return ctx.<Vector2i>getOptional(name)
            .map(loc -> new BlockPos(loc.x(), 0, loc.y()))
            .orElse(null);
    }

    @Override
    public CommandArgument<Commander, ?> singlePlayerSelectorArgument(final String name) {
        return SinglePlayerSelectorArgument.of(name);
    }

    @Override
    public ServerPlayer extractPlayer(final String name, final CommandContext<Commander> ctx) throws CommandCompleted {
        final Commander sender = ctx.getSender();
        final @Nullable SinglePlayerSelector selector = ctx.getOrDefault(name, null);

        if (selector == null) {
            if (sender instanceof PlayerCommander player) {
                return player.player();
            }
            throw CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_PLAYER);
        }

        return (ServerPlayer) selector.getSingle();
    }
}
