package net.pl3x.map.plugin.util;

import cloud.commandframework.bukkit.arguments.selector.SinglePlayerSelector;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.RichDescription;
import java.util.Optional;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.kyori.adventure.text.minimessage.template.TemplateResolver;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.exception.CompletedSuccessfullyException;
import net.pl3x.map.plugin.command.exception.ConsoleMustProvideWorldException;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static @NonNull MapWorld resolveWorld(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final MapWorld world = context.getOrDefault("world", null);
        if (world != null) {
            return world;
        }
        if (sender instanceof Player) {
            final Player player = (Player) sender;
            final World bukkit = player.getWorld();
            Optional<MapWorld> optionalMapWorld = Pl3xMapPlugin.getInstance().worldManager().getWorldIfEnabled(bukkit);
            if (optionalMapWorld.isEmpty()) {
                Lang.send(sender, Lang.MAP_NOT_ENABLED_FOR_WORLD, Template.template("world", bukkit.getName()));
                throw new CompletedSuccessfullyException();
            } else {
                return optionalMapWorld.get();
            }
        } else {
            throw new ConsoleMustProvideWorldException(context);
        }
    }

    public static @NonNull Player resolvePlayer(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        final SinglePlayerSelector selector = context.getOrDefault("player", null);

        if (selector == null) {
            if (sender instanceof Player) {
                return (Player) sender;
            }
            Lang.send(sender, Lang.CONSOLE_MUST_SPECIFY_PLAYER);
            throw new CompletedSuccessfullyException();
        }

        final Player targetPlayer = selector.getPlayer();
        if (targetPlayer == null) {
            Lang.send(sender, Lang.PLAYER_NOT_FOUND_FOR_INPUT, Template.template("input", selector.getSelector()));
            throw new CompletedSuccessfullyException();
        }

        return targetPlayer;
    }

    public static @NonNull RichDescription description(final @NonNull String miniMessage, @NonNull Template @NonNull ... placeholders) {
        return RichDescription.of(MiniMessage.miniMessage().deserialize(miniMessage, TemplateResolver.templates(placeholders)));
    }

}
