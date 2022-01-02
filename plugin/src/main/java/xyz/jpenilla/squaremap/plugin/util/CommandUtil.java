package xyz.jpenilla.squaremap.plugin.util;

import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.RichDescription;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import xyz.jpenilla.squaremap.common.config.Lang;
import xyz.jpenilla.squaremap.plugin.SquaremapPlugin;
import xyz.jpenilla.squaremap.plugin.command.exception.CommandCompleted;
import xyz.jpenilla.squaremap.plugin.data.PaperMapWorld;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static @NonNull PaperMapWorld resolveWorld(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final PaperMapWorld world = context.getOrDefault("world", null);
        if (world != null) {
            return world;
        }
        if (sender instanceof final Player player) {
            final World bukkit = player.getWorld();
            Optional<PaperMapWorld> optionalMapWorld = SquaremapPlugin.getInstance().worldManager().getWorldIfEnabled(bukkit);
            if (optionalMapWorld.isEmpty()) {
                Lang.send(sender, Lang.MAP_NOT_ENABLED_FOR_WORLD, Template.template("world", bukkit.getKey().asString()));
                throw CommandCompleted.withoutMessage();
            } else {
                return optionalMapWorld.get();
            }
        } else {
            throw CommandCompleted.withMessage(MiniMessage.miniMessage().deserialize(Lang.CONSOLE_MUST_SPECIFY_WORLD));
        }
    }

    public static @NonNull Player resolvePlayer(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final SinglePlayerSelector selector = context.getOrDefault("player", null);

        if (selector == null) {
            if (sender instanceof Player player) {
                return player;
            }
            throw CommandCompleted.withMessage(MiniMessage.miniMessage().deserialize(Lang.CONSOLE_MUST_SPECIFY_PLAYER));
        }

        final Player targetPlayer = selector.getPlayer();
        if (targetPlayer == null) {
            Lang.send(sender, Lang.PLAYER_NOT_FOUND_FOR_INPUT, Template.template("input", selector.getSelector()));
            throw CommandCompleted.withoutMessage();
        }

        return targetPlayer;
    }

    public static @NonNull RichDescription description(final @NonNull String miniMessage, @NonNull Template @NonNull ... placeholders) {
        return RichDescription.of(MiniMessage.miniMessage().deserialize(miniMessage, TemplateResolver.templates(placeholders)));
    }

}
