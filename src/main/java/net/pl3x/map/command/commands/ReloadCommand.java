package net.pl3x.map.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.meta.CommandMeta;
import net.pl3x.map.Pl3xMap;
import net.pl3x.map.command.CommandManager;
import net.pl3x.map.command.Pl3xMapCommand;
import net.pl3x.map.configuration.Config;
import net.pl3x.map.configuration.Lang;
import net.pl3x.map.util.FileUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ReloadCommand extends Pl3xMapCommand {

    public ReloadCommand(final @NonNull Pl3xMap plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("reload")
                        .meta(CommandMeta.DESCRIPTION, "Reloads the plugin")
                        .permission("pl3xmap.command.reload")
                        .handler(this::execute));
    }

    public void execute(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        plugin.stop();

        Config.reload();
        Lang.reload();
        FileUtil.reload();

        plugin.start();

        PluginDescriptionFile desc = plugin.getDescription();
        Lang.send(sender, Lang.PLUGIN_RELOADED
                .replace("{name}", desc.getName())
                .replace("{version}", desc.getVersion()));
    }
}
