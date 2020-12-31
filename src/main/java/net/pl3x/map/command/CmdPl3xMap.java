package net.pl3x.map.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.RenderManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginDescriptionFile;

public class CmdPl3xMap implements TabExecutor {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            String arg = args[0].toLowerCase(Locale.ROOT);
            return Stream.of("fullrender")
                    .filter(cmd -> cmd.startsWith(arg))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            String arg = args[1].toLowerCase(Locale.ROOT);
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(arg))
                    .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0) {
            if (args[0].equalsIgnoreCase("fullrender")) {
                World world = getWorld(sender, args);
                if (world == null) {
                    return true;
                }

                if (RenderManager.isRendering(world)) {
                    sender.sendMessage(ChatColor.RED + "A render is already in progress on " + world.getName());
                    return true;
                }

                sender.sendMessage(ChatColor.GREEN + "Full render started on " + world.getName());
                RenderManager.fullRender(world);
                return true;
            }

            sender.sendMessage(ChatColor.RED + "Unknown subcommand");
            return false;
        }

        PluginDescriptionFile desc = Pl3xMap.getInstance().getDescription();
        sender.sendMessage(ChatColor.GREEN + desc.getName() + " v" + desc.getVersion());
        return true;
    }

    private World getWorld(CommandSender sender, String[] args) {
        World world;
        if (args.length > 1) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                sender.sendMessage(ChatColor.RED + "World not found");
                return null;
            }
        } else if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            sender.sendMessage(ChatColor.RED + "Must specify a world");
            return null;
        }
        return world;
    }
}
