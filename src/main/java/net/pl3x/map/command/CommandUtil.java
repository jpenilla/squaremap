package net.pl3x.map.command;

import cloud.commandframework.context.CommandContext;
import net.pl3x.map.command.exception.ConsoleMustProvidePlayerException;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class CommandUtil {
    private CommandUtil() {
    }

    public static @NonNull World getWorldIfPlayer(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        if (sender instanceof Player) {
            return ((Player) sender).getWorld();
        }
        throw new ConsoleMustProvidePlayerException();
    }
}
