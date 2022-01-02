package xyz.jpenilla.squaremap.common.util;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.RichDescription;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import net.minecraft.server.level.ServerLevel;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.command.Commander;
import xyz.jpenilla.squaremap.common.command.Commands;
import xyz.jpenilla.squaremap.common.command.PlayerCommander;
import xyz.jpenilla.squaremap.common.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.common.data.MapWorldInternal;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static @NonNull MapWorldInternal resolveWorld(final @NonNull CommandContext<Commander> context) {
        final Commander sender = context.getSender();
        final MapWorldInternal world = context.getOrDefault("world", null);
        if (world != null) {
            return world;
        }
        if (sender instanceof final PlayerCommander player) {
            final ServerLevel level = player.player().getLevel();
            Optional<MapWorldInternal> optionalMapWorld = context.get(Commands.PLATFORM).worldManager().getWorldIfEnabled(level);
            if (optionalMapWorld.isEmpty()) {
                Lang.send(sender, Lang.MAP_NOT_ENABLED_FOR_WORLD, Template.template("world", level.dimension().location().toString()));
                throw CommandCompleted.withoutMessage();
            } else {
                return optionalMapWorld.get();
            }
        } else {
            throw CommandCompleted.withMessage(MiniMessage.miniMessage().deserialize(Lang.CONSOLE_MUST_SPECIFY_WORLD));
        }
    }

    public static @NonNull RichDescription description(final @NonNull String miniMessage, @NonNull Template @NonNull ... placeholders) {
        return RichDescription.of(MiniMessage.miniMessage().deserialize(miniMessage, TemplateResolver.templates(placeholders)));
    }

}
