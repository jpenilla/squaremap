package net.pl3x.map.plugin.util;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.RichDescription;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.Template;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.exception.CompletedSuccessfullyException;
import net.pl3x.map.plugin.command.exception.ConsoleMustProvideWorldException;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.data.MapWorld;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

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
                Lang.send(sender, Lang.MAP_NOT_ENABLED_FOR_WORLD, Template.of("world", bukkit.getName()));
                throw new CompletedSuccessfullyException();
            } else {
                return optionalMapWorld.get();
            }
        } else {
            throw new ConsoleMustProvideWorldException(context);
        }
    }

    public static @NonNull RichDescription description(final @NonNull String miniMessage) {
        return RichDescription.of(MiniMessage.get().parse(miniMessage));
    }

}
