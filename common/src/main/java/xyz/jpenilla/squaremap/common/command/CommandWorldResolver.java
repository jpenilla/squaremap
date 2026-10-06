package xyz.jpenilla.squaremap.common.command;

import java.util.Optional;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.incendo.cloud.context.CommandContext;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Messages;
import xyz.jpenilla.squaremap.common.util.text.Components;
import xyz.jpenilla.squaremap.common.world.MapWorldInternal;

@DefaultQualifier(NonNull.class)
public final class CommandWorldResolver {
    private CommandWorldResolver() {
    }

    public static MapWorldInternal resolveWorld(final CommandContext<Commander> context) {
        final Commander sender = context.sender();
        final @Nullable MapWorldInternal world = context.getOrDefault("world", null);
        if (world != null) {
            return world;
        }
        if (sender instanceof final PlayerCommander player) {
            final ServerLevel level = player.player().level();
            final Optional<MapWorldInternal> mapWorld = context.get(Commands.WORLD_MANAGER).getWorldIfEnabled(level);
            if (mapWorld.isPresent()) {
                return mapWorld.get();
            }
            throw CommandCompleted.withMessage(
                Messages.MAP_NOT_ENABLED_FOR_WORLD.withPlaceholders(Components.worldPlaceholder(level))
                    .color(NamedTextColor.RED)
            );
        } else {
            throw CommandCompleted.withMessage(Messages.CONSOLE_MUST_SPECIFY_WORLD);
        }
    }
}
