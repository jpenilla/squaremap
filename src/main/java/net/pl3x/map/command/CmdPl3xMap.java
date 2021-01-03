package net.pl3x.map.command;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.RenderManager;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.Bukkit;
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
            return Stream.of("fullrender", "reload")
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
                    Lang.send(sender, Lang.RENDER_IN_PROGRESS
                            .replace("{world}", world.getName()));
                    return true;
                }

                Lang.send(sender, Lang.FULL_RENDER_STARTED
                        .replace("{world}", world.getName()));
                RenderManager.fullRender(world);
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                Pl3xMap.getInstance().stop();

                Config.reload();
                Lang.reload();
                FileUtil.reload();

                Pl3xMap.getInstance().start();

                PluginDescriptionFile desc = Pl3xMap.getInstance().getDescription();
                Lang.send(sender, Lang.PLUGIN_RELOADED
                        .replace("{name}", desc.getName())
                        .replace("{version}", desc.getVersion()));
                return true;
            }

            Lang.send(sender, Lang.UNKNOWN_SUBCOMMAND);
            return false;
        }

        PluginDescriptionFile desc = Pl3xMap.getInstance().getDescription();
        Lang.send(sender, Lang.PLUGIN_VERSION
                .replace("{name}", desc.getName())
                .replace("{version}", desc.getVersion()));
        return true;
    }

    private World getWorld(CommandSender sender, String[] args) {
        World world;
        if (args.length > 1) {
            world = Bukkit.getWorld(args[1]);
            if (world == null) {
                Lang.send(sender, Lang.WORLD_NOT_FOUND);
                return null;
            }
        } else if (sender instanceof Player) {
            world = ((Player) sender).getWorld();
        } else {
            Lang.send(sender, Lang.WORLD_NOT_SPECIFIED);
            return null;
        }
        return world;
    }
}
